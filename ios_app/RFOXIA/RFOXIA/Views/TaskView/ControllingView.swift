//
//  ControllingView.swift
//  RFOXIA
//
//  Created by Kerlos on 02/05/2025.
//

import SwiftUI
import MapKit

struct ControllingView: View {
    var latitude: Double
    var longitude: Double

    @State private var region: MKCoordinateRegion

    init(latitude: Double, longitude: Double) {
        self.latitude = latitude
        self.longitude = longitude

        // Initialize the region with received coordinates
        _region = State(initialValue: MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
        ))
    }
    
    var body: some View {
        ZStack {
            // Map as background
            Map(coordinateRegion: $region)
                .edgesIgnoringSafeArea(.all)

            // Example overlay content
            VStack {
                JoyStickView()
                Spacer()
            }
            .padding()
        }
    }
}

#Preview {
    ControllingView(latitude: 40.7128, longitude: -74.0060)
}
