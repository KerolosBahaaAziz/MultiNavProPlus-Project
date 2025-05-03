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
    @State var tasks: [Task] = [
        Task(action: "action1"), Task(action: "action2"), Task(action: "action3"),
        Task(action: "action4"), Task(action: "action5"), Task(action: "action6"),
        Task(action: "action7"), Task(action: "action8"), Task(action: "action9"),
        Task(action: "action10")
    ]
    @EnvironmentObject var bluetoothManager: BluetoothManager
    @State var temp: Int = 35
    @State private var isPortrait: Bool = false
    
    @State private var latitude: Double = 40.7128
    @State private var longitude: Double = -74.006
    @State private var mapRegion = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 40.7128, longitude: -74.006), 
        span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
    )
    
    var body: some View {
        NavigationStack {
            ZStack{
                Map(coordinateRegion: $mapRegion)
                    .ignoresSafeArea()
                GeometryReader { geometry in
                    Group {
                        if isPortrait {
                            VStack {
                                Spacer()
                                Text("Please rotate your phone")
                                    .font(.largeTitle)
                                    .bold()
                                    .multilineTextAlignment(.center)
                                    .padding()
                                Text("This screen is better used in landscape mode.")
                                    .font(.title2)
                                    .foregroundStyle(.secondary)
                                    .multilineTextAlignment(.center)
                                    .padding()
                                
                                Image(systemName: "iphone.landscape")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 150, height: 150) // or any size you like
                                    .foregroundColor(.gray)
                                    .padding()
                                Spacer()
                            }
                            .frame(width: geometry.size.width, height: geometry.size.height)
                            .background(Color(.systemBackground)) // match system background color
                        } else {
                            VStack(spacing: 16) {
                                SensorsReadingView(temp: "\(temp)", humidity: "48", pressure: "1013", status: "Good")
                                
                                ModeButtonsView(selectedIndex: $selectedMode)
                                    .padding(.horizontal)
                                    .frame(height: geometry.size.height * 0.05)
                                
                                Text("\(value)")
                                    .font(.title)
                                    .padding(.vertical, 10)
                                
                                HStack {
                                    DirectionPadView { direction in
                                        print("direction is \(direction)")
                                        bluetoothManager.sendCommandToMicrocontroller("ol")
                                    }
                                    Spacer()
                                    rotatingKnobView(selection: $value, range: -500...500) { isMoving in
                                        print("isMoving: \(isMoving)")
                                    }
                                    .aspectRatio(1, contentMode: .fit)
                                    .frame(maxWidth: geometry.size.width * 0.3)
                                    .padding(.horizontal)
                                    Spacer()
                                    ActionButtonsView { action in
                                        bluetoothManager.sendCommandToMicrocontroller("fd")
                                        print("Action is \(action)")
                                    }
                                }
                                .frame(maxHeight: geometry.size.height * 0.25)
                                
                                Spacer()
                                ActivatorButtonView { action, isActivated in
                                    bluetoothManager.sendCommandToMicrocontroller("fl")
                                    print("\(action) is \(isActivated)")
                                }
                                .padding(.bottom)
                            }
                            .padding()
                            .frame(width: geometry.size.width, height: geometry.size.height)
                            .overlay(
                                NavigationLink(destination: TaskView()) {
                                    Image(systemName: "plusminus.circle.fill")
                                        .font(.system(size: 40))
                                        .foregroundStyle((BackgroundGradient.backgroundGradient))
                                }
                                , alignment: .bottomTrailing
                            )
                        }
                    }
                }
            }
            .onAppear {
                bluetoothManager.enableNotify(for: [bluetoothManager.accelerometerCharacteristicUUID])
                checkOrientation()
            }
            .onRotate { newOrientation in
                checkOrientation()
            }
            .onDisappear {
                bluetoothManager.disableNotify(for: [bluetoothManager.accelerometerCharacteristicUUID])
            }
            .onChange(of: selectedMode) { oldValue, newValue in
                if oldValue != newValue {
                    print("selectedMode changed to \(newValue + 1)")
                }
            }
            .onReceive(bluetoothManager.$accelerometerMessages) { message in
                temp = message
            }
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
