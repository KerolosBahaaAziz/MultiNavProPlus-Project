//
//  BluetoothPresenter.swift
//  RFOXIA
//
//  Created by Kerlos on 22/04/2025.
//

import Foundation
class BluetoothPresenter{
    var bluetoothManager = BluetoothManager()
    
    func sendMessage(message : String){
        bluetoothManager.sendMessage(message)
    }
}
