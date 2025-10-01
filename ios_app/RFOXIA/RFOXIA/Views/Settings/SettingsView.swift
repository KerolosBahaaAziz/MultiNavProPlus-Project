//
//  SettingsView.swift
//  RFOXIA
//
//  Created by Kerlos on 29/09/2025.
//

import Foundation
import SwiftUI
import PhotosUI
import FirebaseAuth

struct SettingsView: View {
    @State private var email: String = (UserDefaults.standard.string(forKey: "userEmail") ?? "")
    @State private var password: String = (UserDefaults.standard.string(forKey: "userPassword") ?? "")
    @State private var isPasswordVisible: Bool = false
    @State private var isSubscribed: Bool = UserDefaults.standard.bool(forKey: "isSubscribe")
    @State private var isGotoSubscribed: Bool = false
    
    @State private var selectedItem: PhotosPickerItem? = nil
    @State private var profileImage: Image? = nil
    
    @State private var showChangePassword = false
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var passwordMessage: String? = nil
    @State private var showLogoutAlert = false
    @State private var isLoggedOut = false
    @State private var isPasswordChanged = false
    
    private let profileImageKey = "profileImageData"
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    
                    // MARK: - Profile Section
                    VStack {
                        PhotosPicker(selection: $selectedItem, matching: .images) {
                            if let profileImage = profileImage {
                                profileImage
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 120, height: 120)
                                    .clipShape(Circle())
                                    .overlay(
                                        Circle()
                                            .stroke(isSubscribed ? Color.yellow : Color.gray, lineWidth: 4)
                                    )
                                    .shadow(radius: 5)
                            } else {
                                Image(systemName: "person.crop.circle.fill")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 120, height: 120)
                                    .foregroundColor(.gray)
                                    .overlay(
                                        Circle()
                                            .stroke(isSubscribed ? Color.yellow : Color.gray, lineWidth: 4)
                                    )
                                    .shadow(radius: 5)
                            }
                        }
                        .onChange(of: selectedItem) { newItem in
                            Task {
                                if let data = try? await newItem?.loadTransferable(type: Data.self),
                                   let uiImage = UIImage(data: data) {
                                    profileImage = Image(uiImage: uiImage)
                                    UserDefaults.standard.set(data, forKey: profileImageKey)
                                }
                            }
                        }
                        
                        Text("Welcome!")
                            .font(.title2)
                            .bold()
                        
                        Text(email)
                            .foregroundColor(.secondary)
                    }
                    
                    // MARK: - Account Info Card
                    VStack(spacing: 12) {
                        HStack {
                            Label("Email", systemImage: "envelope.fill")
                                .foregroundColor(.blue)
                            Spacer()
                            Text(email)
                                .foregroundColor(.primary)
                        }
                        
                        Divider()
                        
                        HStack {
                            Label("Password", systemImage: "lock.fill")
                                .foregroundColor(.orange)
                            Spacer()
                            if isPasswordVisible {
                                Text(password)
                            } else {
                                Text(String(repeating: "•", count: password.count))
                            }
                            Button {
                                isPasswordVisible.toggle()
                            } label: {
                                Image(systemName: isPasswordVisible ? "eye.slash.fill" : "eye.fill")
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(16)
                    .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
                    
                    // MARK: - Actions
                    VStack(spacing: 16) {
                        Button(action: {
                            showChangePassword = true
                        }) {
                            HStack {
                                Image(systemName: "key.fill")
                                Text("Change Password")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        
                        Button(action: {
                            isGotoSubscribed.toggle()
                            //UserDefaults.standard.set(isSubscribed, forKey: "isSubscribe")
                        }) {
                            HStack {
                                Image(systemName: isSubscribed ? "xmark.circle.fill" : "star.fill")
                                Text(isSubscribed ? "Cancel Subscription" : "Subscribe")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(isSubscribed ? Color.red : Color.green)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        
                        Button(action: {
                            showLogoutAlert = true
                        }) {
                            HStack {
                                Image(systemName: "arrow.backward.circle.fill")
                                Text("Logout")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.gray)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .alert("Are you sure you want to logout?", isPresented: $showLogoutAlert) {
                            Button("Cancel", role: .cancel) {}
                            Button("Logout", role: .destructive) {
                                logoutUser()
                            }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Settings")
            .onAppear {
                // Load saved profile image
                if let data = UserDefaults.standard.data(forKey: profileImageKey),
                   let uiImage = UIImage(data: data) {
                    profileImage = Image(uiImage: uiImage)
                }
            }
            // MARK: - Change Password Sheet
            .sheet(isPresented: $showChangePassword) {
                VStack(spacing: 20) {
                    Text("Change Password")
                        .font(.title2)
                        .bold()
                    
                    SecureField("New Password", text: $newPassword)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .padding(.horizontal)
                    
                    SecureField("Confirm Password", text: $confirmPassword)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .padding(.horizontal)
                    
                    if let message = passwordMessage {
                        Text(message)
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    
                    Button("Save") {
                        if newPassword.isEmpty || confirmPassword.isEmpty {
                            passwordMessage = "Fields cannot be empty."
                        } else if newPassword != confirmPassword {
                            passwordMessage = "Passwords do not match."
                        } else {
                            changePassword(newPassword: newPassword)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                    .padding(.horizontal)
                    
                    Spacer()
                }
                .padding()
            }
            .fullScreenCover(isPresented: $isLoggedOut) {
                GoogleSignInView()
            }
            .fullScreenCover(isPresented: $isPasswordChanged) {
                GoogleSignInView()
            }
            .fullScreenCover(isPresented: $isGotoSubscribed) {
                ChoosePaymentMethodView()
            }
        }
    }
    
    // MARK: - Firebase Change Password
    private func changePassword(newPassword: String) {
        guard let user = Auth.auth().currentUser else {
            passwordMessage = "No user is logged in."
            return
        }
        
        user.updatePassword(to: newPassword) { error in
            if let error = error {
                passwordMessage = error.localizedDescription
            } else {
                passwordMessage = "Password changed successfully."
                UserDefaults.standard.set(newPassword, forKey: "userPassword")
                UserDefaults.standard.removeObject(forKey: "isLogin")
                isPasswordChanged = true
                
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    showChangePassword = false
                    self.newPassword = ""
                    confirmPassword = ""
                    passwordMessage = nil
                }
            }
        }
    }
    
    // MARK: - Firebase Logout
    private func logoutUser() {
        do {
            try Auth.auth().signOut()
            
            // Clear local storage
            email = ""
            password = ""
            isSubscribed = false
            profileImage = nil
            UserDefaults.standard.removeObject(forKey: "isLogin")
            UserDefaults.standard.removeObject(forKey: "isSubscribed")
            //UserDefaults.standard.removeObject(forKey: profileImageKey)
            UserDefaults.standard.removeObject(forKey: "userEmail")
            UserDefaults.standard.removeObject(forKey: "userPassword")
            
            isLoggedOut = true
            print("User logged out successfully")
        } catch {
            print("Error logging out: \(error.localizedDescription)")
        }
    }
}
