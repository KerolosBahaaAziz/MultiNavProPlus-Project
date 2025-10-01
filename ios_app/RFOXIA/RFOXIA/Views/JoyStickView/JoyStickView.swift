//
//  ContentView.swift
//  Testing JoyStick with SwiftUI
//
//  Created by Yasser Yasser on 11/04/2025.
//

import SwiftUI
import MapKit

struct JoyStickView: View {
    @State var value: Int = 0
    @State var selectedMode: Int = 0
    @Environment(\.scenePhase) var scenePhase
    @EnvironmentObject var bluetoothManager: BluetoothManager
    
    @State private var isPortrait: Bool = false
    @State private var navigateToTask = false
    @State private var navigateToSubscribe = false
    @State private var alertItem: AlertInfo?

    @State private var mapRegion = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 40.7128, longitude: -74.006),
        span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
    )
    
    var body: some View {
        NavigationStack {
            ZStack {
                Map(coordinateRegion: $mapRegion)
                    .ignoresSafeArea()
                
                GeometryReader { geometry in
                    Group {
                        if isPortrait {
                            RotateYourPhoneView()
                                .frame(width: geometry.size.width, height: geometry.size.height)
                        } else {
                            infoSection(size: geometry.size)
                                .frame(width: geometry.size.width, height: geometry.size.height)
                                .ignoresSafeArea(edges: .bottom)
                        }
                    }
                    // 📍 Place overlay OUTSIDE the if-else so it always exists
                    
                }
            }
            
            
            // 📍 Attach alert here too
            .alert(info: $alertItem)
            .onAppear {
                bluetoothManager.enableNotify(for: [
                    bluetoothManager.accelerometerCharacteristicUUID,
                    bluetoothManager.airPressureCharacteristicUUID,
                    bluetoothManager.humidityCharacteristicUUID,
                    bluetoothManager.temperatureCharacteristicUUID
                ])
                checkOrientation()
            }
            .onRotate { _ in
                checkOrientation()
            }
            .onDisappear {
                bluetoothManager.disableNotify(for: [
                    bluetoothManager.accelerometerCharacteristicUUID,
                    bluetoothManager.airPressureCharacteristicUUID,
                    bluetoothManager.humidityCharacteristicUUID,
                    bluetoothManager.temperatureCharacteristicUUID
                ])
            }
            .onChange(of: selectedMode) { oldValue, newValue in
                if oldValue != newValue {
                    print("selectedMode changed to \(newValue + 1)")
                }
            }
        }
    }
    
    @ViewBuilder
    private func infoSection(size: CGSize) -> some View  {
        VStack(spacing: 14) {
            sensorReadings
            modeSelector(size: size)
            valueDisplay
            controlSection
            Spacer()
            activatorButton
        }
        .padding()
        .frame(width: size.width, height: size.height)
        .overlay(
            Button(action: handlePlusTapped) {
                Image(systemName: "plusminus.circle.fill")
                    .font(.system(size: 40))
                    .foregroundStyle(BackgroundGradient.backgroundGradient)
            }
            .padding()
            , alignment: .bottomTrailing
        )
        .background(
            Group {
                NavigationLink("", destination: TaskView(), isActive: $navigateToTask)
                    .hidden()
                NavigationLink("", destination: ChoosePaymentMethodView(), isActive: $navigateToSubscribe)
                    .hidden()
            }
        )
    }
    
    
    @ViewBuilder
    private var controlSection: some View {
        HStack {
            DirectionPadView { direction in
                print("direction is \(direction)")
                bluetoothManager.sendCommandToMicrocontroller("ol")
            }
            Spacer()
            
            rotatingKnobView(selection: $value, range: -600...600) { isMoving in
                print("isMoving: \(isMoving)")
            }
            .aspectRatio(1, contentMode: .fit)
            .frame(maxWidth: UIScreen.main.bounds.width * 0.3)
            .padding(.horizontal)
            
            Spacer()
            
            ActionButtonsView { action in
                bluetoothManager.sendCommandToMicrocontroller("fd")
                print("Action is \(action)")
            }
        }
        .frame(maxHeight: UIScreen.main.bounds.height * 0.25)
    }
    
private func handlePlusTapped() {
    let isSubscribed = UserDefaults.standard.bool(forKey: "isSubscribed")
    let timeStamp = UserDefaults.standard.double(forKey: "subscriptionExpireDate")
    let expiryDate = Date(timeIntervalSince1970: timeStamp)
    let isExpired = expiryDate < Date()
    
    if isSubscribed && !isExpired {
        // ✅ navigate instead of recording
        navigateToTask = true
    } else {
        alertItem = AlertInfo(
            title: "Notice",
            message: isSubscribed
                ? "Your subscription expired. Subscribe now for $1/month."
                : "To use this feature, subscribe for $1/month.",
            confirmText: "Subscribe",
            cancelText: "Cancel",
            confirmAction: {
                navigateToSubscribe = true
            }
        )
    }
}

    
    private func checkOrientation() {
        let orientation = UIDevice.current.orientation
        if orientation == .portrait || orientation == .portraitUpsideDown {
            isPortrait = true
        } else if orientation == .landscapeLeft || orientation == .landscapeRight {
            isPortrait = false
        }
    }

    
    private var sensorReadings: some View {
        HStack{
            if let temp = bluetoothManager.temperatureMessages {
                Text("\(String(format: "%.1f", (temp / 16383.0) * 165.0 - 40.0)) °C")
            } else {
                Text("U °C")
            }
            
            Spacer()
            
            if let humidity = bluetoothManager.humidityMessages {
                Text("\(String(format: "%.1f", (humidity / 16383.0) * 100.0)) %")
            } else {
                Text("U %")
            }
            
            Spacer()
            
            if let pressure = bluetoothManager.airPressureMessages{
                Text("\(String(format: "%.1f", pressure / 4098.0)) hPa")
            } else {
                Text("U hPa")
            }
            
            Spacer()
            
            Text("good")
        }
        .font(.system(size: 33))
        .background(in: .rect, fillStyle: .init(eoFill: true))
    }
    
    private func modeSelector(size: CGSize) -> some View {
        ModeButtonsView(selectedIndex: $selectedMode)
            .padding(.horizontal)
            .frame(height: size.height * 0.05)
    }
    
    private var valueDisplay: some View {
        Text("\(value)")
            .font(.title)
            .padding(.vertical, 10)
    }
    
    private var activatorButton: some View {
        ActivatorButtonView { action, isActivated in
            bluetoothManager.sendCommandToMicrocontroller("fl")
            print("\(action) is \(isActivated)")
        }
        .padding(.bottom)
    }
    
    private var addTaskButton: some View {
        NavigationLink(destination: TaskView()) {
            Image(systemName: "plusminus.circle.fill")
                .font(.system(size: 40))
                .foregroundStyle(BackgroundGradient.backgroundGradient)
        }
    }
    
}


// Extension to detect rotation
extension View {
    func onRotate(perform action: @escaping (UIDeviceOrientation) -> Void) -> some View {
        self
            .onReceive(NotificationCenter.default.publisher(for: UIDevice.orientationDidChangeNotification)) { _ in
                action(UIDevice.current.orientation)
            }
    }
}


#Preview {
    JoyStickView()
        .environmentObject(BluetoothManager())
}
