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
    
    private var airPressureCharacteristic: CBCharacteristic?
    private var temperatureCharacteristic: CBCharacteristic?
    private var humidityCharacteristic: CBCharacteristic?
    private var audioCharacteristic: CBCharacteristic?
    private var deviceNameCharacteristic: CBCharacteristic?
    
    private var expectingDeviceCount = false
    private var expectedDeviceCount = 0
    private var receivedDeviceNamesBuffer = ""
    private var deviceNameBuffer = Data()
    private var counter : Int = 0
    
    private var sendDirectionCharacteristic: CBCharacteristic?
    private var sendToDiscoverDevicesCharacteristic : CBCharacteristic?
    
    private let storage: StorageService
    
    @Published var isBluetoothOn = false
    @Published var isConnected = false
    @Published var discoveredPeripherals: [(CBPeripheral, String)] = []
    @Published var receivedMessages: [String] = []
    
    @Published var lastAccelerometerUpdate: Date? = nil
    @Published var lastTemperatureUpdate: Date? = nil
    @Published var lastHumidityUpdate: Date? = nil
    @Published var lastPressureUpdate: Date? = nil
    
    var safeTemperature: Float? {
        guard let last = lastTemperatureUpdate, Date().timeIntervalSince(last) < 5 else {
            temperatureMessages = nil
            return nil
        }
        return temperatureMessages
    }
    
    var safeHumidity: Float? {
        guard let last = lastHumidityUpdate, Date().timeIntervalSince(last) < 5 else {
            humidityMessages = nil
            return nil
        }
        return humidityMessages
    }
    
    var safePressure: Float? {
        guard let last = lastPressureUpdate, Date().timeIntervalSince(last) < 5 else {
            airPressureMessages = nil
            return nil
        }
        return airPressureMessages
    }
    
    //    {
    //        didSet {
    //            storage.save(receivedMessages, forKey: "receivedMessages")
    //        }
    //    }
    @Published var accelerometerMessages: Int {
        didSet {
            storage.saveInt(accelerometerMessages, forKey: "accelerometerMessages")
        }
    }
    
    @Published var airPressureMessages: Float?
    //    {
    //        didSet {
    //            let realValue: Float = airPressureMessages/4098.0
    //            storage.saveFloat(realValue, forKey: "airPressureMessages")
    //        }
    //    }
    
    @Published var temperatureMessages: Float?
    //    {
    //        didSet {
    //            let realValue: Float = (temperatureMessages / 16383.0) * 165.0 - 40.0
    //            storage.saveFloat(realValue, forKey: "temperatureMessages")
    //        }
    //    }
    
    @Published var humidityMessages: Float?
    //    {
    //        didSet {
    //            let realValue: Float = (humidityMessages / 16383.0) * 100.0
    //            storage.saveFloat(realValue, forKey: "humidityMessages")
    //        }
    //    }
    
    @Published var nearByDevicesName : [String] = []
    
    @Published var connectedDeviceName: String {
        didSet {
            storage.saveString(connectedDeviceName, forKey: "connectedDeviceName")
        }
    }
    @Published var connectionErrorMessage: String = ""
    @Published var showConnectionError: Bool = false
    @Published var receivedAudioData: [Data] = []
    private var currentAudioBuffer: [Data] = []
    // NEW: Added buffer to store incoming message chunks
    private var currentMessageBuffer: [Data] = []
    
    let chatCharacteristicUUID = CBUUID(string: "1234")
    
    let accelerometerCharacteristicUUID = CBUUID(string: "0000FE42-8E22-4541-9D4C-21EDAE82ED19")
    let deviceNameCharacteristicUUID = CBUUID(string: "0240BC9A-7856-3412-7856-341278563412") //replace with the real one
    let sendToDiscoverDevicesCharacteristicUUID = CBUUID(string: "0140BC9A-7856-3412-7856-341278563412") //replace with the real one
    
    
    let airPressureCharacteristicUUID = CBUUID(string: "0320BC9A-7856-3412-7856-341278563412")
    let temperatureCharacteristicUUID = CBUUID(string: "0122BC9A-7856-3412-7856-341278563412")
    let humidityCharacteristicUUID = CBUUID(string: "0222BC9A-7856-3412-7856-341278563412")
    let audioCharacteristicUUID = CBUUID(string: "0000FD43-8E22-4541-9D4C-21EDAE82ED19") // Replace with correct UUID
    
    
    let sendDirectionCharacteristicUUID = CBUUID(string: "0130BC9A-7856-3412-7856-341278563412")
    
    
    private var notifyCapableCharacteristics: [CBUUID: CBCharacteristic] = [:]
    
    init(storage: StorageService = StorageService()) {
        self.storage = storage
        self.connectedDeviceName = storage.loadString(forKey: "connectedDeviceName") ?? "Unknown Device"
        self.receivedMessages = storage.load([String].self, forKey: "receivedMessages") ?? []
        self.accelerometerMessages = storage.loadInt(forKey: "accelerometerMessages")
        
        //        self.airPressureMessages = storage.loadFloat(forKey: "airPressureMessages")
        //        self.temperatureMessages = storage.loadFloat(forKey: "temperatureMessages")
        //        self.humidityMessages = storage.loadFloat(forKey: "humidityMessages")
        
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
    
    func sendMessage(_ message: String) {
        guard let peripheral = connectedPeripheral, let characteristic = messageCharacteristic else {
            print("Bluetooth connection or characteristic not available")
            return
        }
        
        //<<<<<<< HEAD
        //        if let messageData = message.data(using: .utf8){
        //            peripheral.writeValue(messageData, for: characteristic, type: .withoutResponse)
        //=======
        // MODIFIED: Updated to send message in chunks based on MTU
        guard let messageData = message.data(using: .utf8) else {
            print("Error: Couldn't convert message to Data.")
            return
            //>>>>>>> sending-task-history-to-stm
        }
        
        // NEW: Get MTU and send message in chunks
        let mtu = peripheral.maximumWriteValueLength(for: .withoutResponse)
        var offset = 0
        
        while offset < messageData.count {
            let chunkSize = min(mtu, messageData.count - offset)
            let chunk = messageData.subdata(in: offset..<offset + chunkSize)
            peripheral.writeValue(chunk, for: characteristic, type: .withoutResponse)
            print("📤 Sent message chunk of size \(chunk.count) bytes")
            offset += chunkSize
        }
        
        // NEW: Send empty packet to indicate end of message
        peripheral.writeValue(Data(), for: characteristic, type: .withoutResponse)
        print("📤 Sent empty packet to indicate end of message")
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
        self.sendToDiscoverDevicesCharacteristic = nil
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
            print("📏 MTU (write without response): \(peripheral.maximumWriteValueLength(for: .withoutResponse)) bytes")
            print("📏 MTU (write with response): \(peripheral.maximumWriteValueLength(for: .withResponse)) bytes")
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
                peripheral.setNotifyValue(true, for: characteristic)
            }else if characteristic.uuid == airPressureCharacteristicUUID{
                airPressureCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            }else if characteristic.uuid == temperatureCharacteristicUUID{
                temperatureCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            }else if characteristic.uuid == humidityCharacteristicUUID{
                humidityCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            }else if characteristic.uuid == audioCharacteristicUUID {
                audioCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            }else if characteristic.uuid == deviceNameCharacteristicUUID {
                deviceNameCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            }else if characteristic.uuid == sendToDiscoverDevicesCharacteristicUUID {
                sendToDiscoverDevicesCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            }
            
            if characteristic.properties.contains(.notify) {
                notifyCapableCharacteristics[characteristic.uuid] = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
                // Don't subscribe yet — do it when user enters the screen
            } else if characteristic.properties.contains(.read) {
                print("📖 Reading value for \(characteristic.uuid)")
                peripheral.readValue(for: characteristic)
            }
            
        }
    }
    
    // Receiving data from peripheral (Bluetooth)
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard error == nil else { return }
        
        if characteristic == messageCharacteristic {
            // MODIFIED: Updated to handle message chunks and empty packet
            if let data = characteristic.value {
                if data.isEmpty {
                    print("📩 End of message stream received")
                    // NEW: Combine buffered chunks into a single message
                    let combinedData = Data(currentMessageBuffer.joined())
                    if let message = String(data: combinedData, encoding: .utf8) {
                        print("✅ Reconstructed message: \(message)")
                        DispatchQueue.main.async {
                            self.receivedMessages.append(message)
                            self.currentMessageBuffer.removeAll()
                        }
                    } else {
                        print("❌ Failed to decode message as UTF-8 string")
                        DispatchQueue.main.async {
                            self.currentMessageBuffer.removeAll()
                        }
                    }
                } else {
                    print("📩 Received message chunk of size \(data.count) bytes")
                    // NEW: Append chunk to message buffer
                    currentMessageBuffer.append(data)
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
                    self.lastAccelerometerUpdate = Date()
                }
            }
        }
        else if characteristic == airPressureCharacteristic {
            if let data = characteristic.value, data.count >= 3 {
                print("Received raw bytes:", data.map { String(format: "%02hhx", $0) }.joined(separator: " "))
                
                // Reorder to [3e, 8e, 6b]
                let b0 = UInt32(data[2]) // Most significant byte
                let b1 = UInt32(data[1])
                let b2 = UInt32(data[0]) // Least significant byte
                
                // Combine as 24-bit unsigned integer (big endian assumed)
                let rawValue = (b0 << 16) | (b1 << 8) | b2
                
                print("✅ Reconstructed 24-bit airPressure value: \(rawValue)")
                
                DispatchQueue.main.async {
                    self.airPressureMessages = Float(rawValue)
                    self.lastPressureUpdate = Date()
                }
            }
        }
        else if characteristic == temperatureCharacteristic {
            if let data = characteristic.value{
                print("Received raw bytes:", data.map { String(format: "%02hhx", $0) }.joined(separator: " "))
                let value = data.withUnsafeBytes {
                    $0.load(as: Int16.self)
                }
                print("✅ Received Temprature value: \(value)")
                DispatchQueue.main.async {
                    self.temperatureMessages = Float(value)
                    self.lastTemperatureUpdate = Date()
                }
            }
        }
        else if characteristic == humidityCharacteristic {
            if let data = characteristic.value{
                print("Received raw bytes:", data.map { String(format: "%02hhx", $0) }.joined(separator: " "))
                let value = data.withUnsafeBytes {
                    $0.load(as: Int16.self)
                }
                print("✅ Received Humidity value: \(value)")
                DispatchQueue.main.async {
                    self.humidityMessages = Float(value)
                    self.lastHumidityUpdate = Date()
                }
            }
        }
        else if characteristic == deviceNameCharacteristic {
            if let data = characteristic.value {
                print("📦 Chunk received (\(counter + 1)):", data.map { String(format: "%02hhx", $0) }.joined(separator: " "))
                
                // Append new chunk to buffer
                deviceNameBuffer.append(data)
                counter += 1
                processDeviceNameBuffer()
                // Check if this chunk contains the end marker (0x00)
//                if data.contains(0x00) {
//                    print("🔚 End of transmission marker found in current chunk")
//                    processDeviceNameBuffer()
//                }
//                // Also check if the accumulated buffer contains the end marker
//                else if deviceNameBuffer.contains(0x00) {
//                    print("🔚 End of transmission marker found in accumulated buffer")
//                    processDeviceNameBuffer()
//                }
//                // If we've received many chunks but no end marker, assume transmission is complete
//                else if counter > 10 { // Adjust this threshold as needed
//                    print("⚠️ No end marker found after \(counter) chunks, processing anyway")
//                    processDeviceNameBuffer()
//                }
            }
        }
        else if characteristic == audioCharacteristic {
            if let data = characteristic.value {
                if data.isEmpty {
                    print("🔚 End of audio stream received")
                    DispatchQueue.main.async {
                        self.receivedAudioData = self.currentAudioBuffer
                        self.currentAudioBuffer.removeAll()
                    }
                } else {
                    print("🎵 Received audio chunk of size \(data.count) bytes")
                    currentAudioBuffer.append(data)
                }
            }
        }
        if let data = characteristic.value {
            print("📦 Read \(data.count) bytes from \(characteristic.uuid):",
                  data.map { String(format: "%02hhx", $0) }.joined(separator: " "))
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
    
    func sendVoiceData(_ data: Data) {
        guard let peripheral = connectedPeripheral , let characteristic = messageCharacteristic else {
            print("Bluetooth connection or characteristic not available")
            return
        }
        
        let mtu = peripheral.maximumWriteValueLength(for: .withoutResponse)  // Adjust based on negotiated MTU size if needed
        var offset = 0
        
        while offset < data.count {
            let chunkSize = min(mtu, data.count - offset)
            let chunk = data.subdata(in: offset..<offset + chunkSize)
            peripheral.writeValue(chunk, for: characteristic, type: .withoutResponse)
            offset += chunkSize
        }
        
        // Send empty packet to indicate the end of transmission
        peripheral.writeValue(Data(), for: characteristic, type: .withoutResponse)
        print("Voice data sent in chunks with final empty packet.")
    }
    
    func discoverOrSelectNearbyDevices(_ message: Int = 99) {
        guard let peripheral = connectedPeripheral,
              let characteristic = sendToDiscoverDevicesCharacteristic,
              let _ = deviceNameCharacteristic else {
            print("❌ Bluetooth connection or characteristics not available")
            return
        }
        
        // 🧹 Clear app-side buffer
        DispatchQueue.main.async {
            self.deviceNameBuffer.removeAll()
            self.counter = 0
            self.nearByDevicesName.removeAll()
            print("🧹 Cleared buffers for new discovery")
        }
        
        // Step 1: Send ASCII 'c' (0x63 or 99 in decimal) to trigger device discovery
        if message == 99 {
            let discoverCommand = Data([0x63]) // ASCII value of 'c'
            peripheral.writeValue(discoverCommand, for: characteristic, type: .withoutResponse)
            print("📡 Sent discovery command 'c' (0x63)")
            
            // Give some time for the device to start sending data
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                // The device should automatically send chunks via notifications
                print("📡 Waiting for device name data via notifications...")
            }
        }
        // Step 2: Send the device index if message is a valid index
        else if !nearByDevicesName.isEmpty && message >= 0 && message < nearByDevicesName.count {
            let indexData = withUnsafeBytes(of: UInt8(message)) { Data($0) }
            peripheral.writeValue(indexData, for: characteristic, type: .withoutResponse)
            print("📡 Sent device selection index: \(message) for device: \(nearByDevicesName[message])")
        } else {
            print("❌ Invalid device index: \(message). Valid range: 0 to \(nearByDevicesName.count - 1) or no devices discovered")
        }
    }
    
    // Helper function to process the device name buffer
    private func processDeviceNameBuffer() {
        guard connectedPeripheral != nil else { return }
        
        // Find the position of the end marker if it exists
        if let endIndex = deviceNameBuffer.firstIndex(of: 0x00) {
            let payload = deviceNameBuffer.prefix(upTo: endIndex)
            parseDeviceNames(from: payload)
        } else {
            // No end marker found, process the entire buffer
            parseDeviceNames(from: deviceNameBuffer)
        }
        
        // Reset buffer for next transmission
        deviceNameBuffer.removeAll()
        counter = 0
    }
    
    // Helper function to parse device names from data
    private func parseDeviceNames(from data: Data) {
        if data.count > 1 {
            let numberOfDevices = Int(data[0])
            let namesData = data.dropFirst()
            
            if let namesString = String(data: namesData, encoding: .utf8)?
                .replacingOccurrences(of: "\0", with: "") {
                
                let deviceNames = namesString
                    .components(separatedBy: "\n")
                    .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                    .filter { !$0.isEmpty }
                
                if deviceNames.count == numberOfDevices {
                    print("✅ Received \(numberOfDevices) device names: \(deviceNames)")
                } else {
                    print("⚠️ Expected \(numberOfDevices), but got \(deviceNames.count). Names: \(deviceNames)")
                }
                
                DispatchQueue.main.async {
                    self.nearByDevicesName = deviceNames
                }
            } else {
                print("❌ Failed to decode UTF-8 device names")
            }
        }
    }
}
