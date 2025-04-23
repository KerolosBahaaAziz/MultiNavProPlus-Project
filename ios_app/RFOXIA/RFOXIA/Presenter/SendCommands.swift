//
//  SendCommands.swift
//  RFOXIA
//
//  Created by Kerlos on 23/04/2025.
//

import Foundation

class SendCommands{
    var blutoothManager = BluetoothManager()
    
    func sendCommand(command:String){
        blutoothManager.sendCommandToMicrocontroller(command)
    }
}
