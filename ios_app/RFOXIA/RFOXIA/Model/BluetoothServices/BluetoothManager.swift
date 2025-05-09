//
//  BluetoothManager.swift
//  RFOXIA
//
//  Created by Kerlos on 12/04/2025.
//
import Foundation
import CoreBluetooth

class BluetoothManager: NSObject, ObservableObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    private var centralManager: CBCentralManager!
    var connectedPeripheral: CBPeripheral?
    private var messageCharacteristic: CBCharacteristic?
    private var accelerometerCharacteristic: CBCharacteristic?
    private var sendDirectionCharacteristic: CBCharacteristic?
    
    private let storage: StorageService
    
    @Published var isBluetoothOn = false
    @Published var isConnected = false
    @Published var discoveredPeripherals: [(CBPeripheral, String)] = []
    @Published var receivedMessages: [String] {
        didSet {
            storage.save(receivedMessages, forKey: "receivedMessages")
        }
    }
    @Published var accelerometerMessages: Int {
        didSet {
            storage.saveInt(accelerometerMessages, forKey: "accelerometerMessages")
        }
    }
    @Published var connectedDeviceName: String {
        didSet {
            storage.saveString(connectedDeviceName, forKey: "connectedDeviceName")
        }
    }
    @Published var connectionErrorMessage: String = ""
    @Published var showConnectionError: Bool = false
    
    let chatCharacteristicUUID = CBUUID(string: "1234")
    
    let accelerometerCharacteristicUUID = CBUUID(string: "0000FE42-8E22-4541-9D4C-21EDAE82ED19")
    let sendDirectionCharacteristicUUID = CBUUID(string: "0000FE41-8E22-4541-9D4C-21EDAE82ED19")
    
    
    private var notifyCapableCharacteristics: [CBUUID: CBCharacteristic] = [:]
    
    init(storage: StorageService = StorageService()) {
        self.storage = storage
        self.connectedDeviceName = storage.loadString(forKey: "connectedDeviceName") ?? "Unknown Device"
        self.receivedMessages = storage.load([String].self, forKey: "receivedMessages") ?? []
        self.accelerometerMessages = storage.loadInt(forKey: "accelerometerMessages")
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }
    
    //    override init() {
    //        super.init()
    //        centralManager = CBCentralManager(delegate: self, queue: nil)
    //    }
    //
    // Start scanning for peripherals
    func scanForDevices() {
        centralManager.scanForPeripherals(withServices: nil, options: nil)
    }
    
    // Connect to the peripheral
    func connectToPeripheral(_ peripheral: CBPeripheral) {
        centralManager.connect(peripheral, options: nil)
        connectedPeripheral = peripheral
        connectedPeripheral?.delegate = self
    }
    
    // Sending message via Bluetooth
    func sendMessage(_ message: String) {
        guard let peripheral = connectedPeripheral , let characteristic = messageCharacteristic else {
            print("Bluetooth connection or characteristic not available")
            return
        }
        
        if let messageData = message.data(using: .utf8){
            peripheral.writeValue(messageData, for: characteristic, type: .withoutResponse)
        }
    }
    
    func sendCommandToMicrocontroller(_ command: String) {
        guard let peripheral = connectedPeripheral,
              let characteristic = sendDirectionCharacteristic else {
            print("Error: Missing peripheral or characteristic.")
            return
        }
        
        // Get the first 2 characters of the string
        let trimmedCommand = String(command.prefix(2))
        
        guard trimmedCommand.utf8.count == 2 else {
            print("❌ Command contains non-ASCII characters")
            return
        }
        // Convert to Data using UTF-8 encoding
        guard let commandData = trimmedCommand.data(using: .utf8) else {
            print("Error: Couldn't convert command to Data.")
            return
        }
        
        peripheral.writeValue(commandData, for: characteristic, type: .withoutResponse)
    }
    
    // MARK: - CBCentralManagerDelegate
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        isBluetoothOn = central.state == .poweredOn
        
        if isBluetoothOn {
            scanForDevices()
        } else {
            discoveredPeripherals.removeAll()
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral,
                        advertisementData: [String : Any], rssi RSSI: NSNumber) {
        let name = peripheral.name ?? (advertisementData[CBAdvertisementDataLocalNameKey] as? String) ?? "Unknown Device"
        
        if !discoveredPeripherals.contains(where: { $0.0.identifier == peripheral.identifier }) {
            print("Found peripheral: \(peripheral), name: \(peripheral.name ?? "No Name")")
            DispatchQueue.main.async {
                self.discoveredPeripherals.append((peripheral, name))
            }
        }
        print("Found peripheral: \(peripheral), name: \(peripheral.name ?? "No Name")")
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        peripheral.discoverServices(nil)
        centralManager.stopScan()
        isBluetoothOn = true
        connectedDeviceName = peripheral.name ?? "unknown"
        DispatchQueue.main.async {
            self.isConnected = true
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        isBluetoothOn = false
        self.connectedPeripheral = nil
        self.messageCharacteristic = nil
        self.sendDirectionCharacteristic = nil
        DispatchQueue.main.async {
            self.isConnected = false
        }
    }
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        print("Failed to connect to peripheral: \(error?.localizedDescription ?? "Unknown error")")
        self.connectionErrorMessage = error?.localizedDescription ?? "Unknown error"
        self.showConnectionError = true
    }
    
    // MARK: - CBPeripheralDelegate
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard error == nil else { return }
        
        // Discover characteristics for each service
        for service in peripheral.services ?? [] {
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard error == nil else { return }
        for characteristic in service.characteristics ?? [] {
            print("📡 Discovered characteristic: \(characteristic.uuid)")
        }
        print("===============================================")
        // Assuming we have a known characteristic for message exchange
        for characteristic in service.characteristics ?? [] {
            if characteristic.uuid == chatCharacteristicUUID {
                messageCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            }else if characteristic.uuid == accelerometerCharacteristicUUID{
                accelerometerCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            }else if characteristic.uuid == sendDirectionCharacteristicUUID{
                sendDirectionCharacteristic = characteristic
            }
            
            if characteristic.properties.contains(.notify) {
                notifyCapableCharacteristics[characteristic.uuid] = characteristic
                // Don't subscribe yet — do it when user enters the screen
            }
        }
    }
    
    // Receiving data from peripheral (Bluetooth)
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard error == nil else { return }
        
        if characteristic == messageCharacteristic {
            if let data = characteristic.value{
                print("Received raw bytes:", data.map { String(format: "%02hhx", $0) }.joined(separator: " "))
                if let message = String(data: data, encoding: .utf8) {
                    DispatchQueue.main.async {
                        self.receivedMessages.append(message)
                    }
                }
            }
        } else if characteristic == accelerometerCharacteristic {
            if let data = characteristic.value{
                print("Received raw bytes:", data.map { String(format: "%02hhx", $0) }.joined(separator: " "))
                let value = data.withUnsafeBytes {
                            $0.load(as: Int32.self)
                        }
                print("✅ Received accelerometer value: \(value)")
                DispatchQueue.main.async {
                    self.accelerometerMessages = Int(value)
                }
            }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        if let error = error {
            print("Notification setup failed for: \(characteristic.uuid) and error is: \(error.localizedDescription)")
        } else if characteristic.isNotifying {
            print("✅ Subscribed to notifications for \(characteristic.uuid)")
        } else {
            print("Notification set up for \(characteristic.uuid)")
        }
    }
    
    func enableNotifyForAll() {
        guard let peripheral = connectedPeripheral else { return }
        
        for (_, characteristic) in notifyCapableCharacteristics {
            peripheral.setNotifyValue(true, for: characteristic)
        }
    }
    
    func disableNotifyForAll() {
        guard let peripheral = connectedPeripheral else { return }
        
        for (_, characteristic) in notifyCapableCharacteristics {
            peripheral.setNotifyValue(false, for: characteristic)
        }
    }
    
    func enableNotify(for uuids: [CBUUID]) {
        guard let peripheral = connectedPeripheral else { return }
        
        for uuid in uuids {
            if let characteristic = notifyCapableCharacteristics[uuid] {
                peripheral.setNotifyValue(true, for: characteristic)
            }
        }
    }
    
    func disableNotify(for uuids: [CBUUID]) {
        guard let peripheral = connectedPeripheral else { return }
        
        for uuid in uuids {
            if let characteristic = notifyCapableCharacteristics[uuid] {
                peripheral.setNotifyValue(false, for: characteristic)
            }
        }
    }
    
}
