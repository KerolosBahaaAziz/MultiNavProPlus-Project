//
//  LoadingScreenView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 08/05/2025.
//

import SwiftUI

struct LoadingScreenView: View {
    @State private var isLoading = true

    var body: some View {
        ZStack {
            if isLoading {
                // Background Lottie animation
                LottieView(animationName: "splash", loopMode: .loop)
                    .ignoresSafeArea()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Centered logo on top
                Image("logowithoutbg")
                    .resizable()
                    .scaledToFit()
            } else {
                GoogleSignInView()
            }
        }
        .onAppear {
            simulateLoading()
        }
    }

    private func simulateLoading() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 6) {
            withAnimation {
                isLoading = false
            }
        }
    }
}

#Preview {
    LoadingScreenView()
}
