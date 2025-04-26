//
//  RegisterView.swift
//  RFOXIA
//
//  Created by Kerlos on 26/04/2025.
//

import SwiftUI

struct RegisterView: View {
    @Environment(\.presentationMode) var presentationMode
    
    @State private var firstName: String = ""
    @State private var lastName: String = ""
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showAlert = false
    @State private var registrationSuccess = false
    @State private var alertMessage = ""
    @State private var navigateToSignIn = false // Add this state

    var body: some View {
        NavigationStack { 
            ZStack {
                VStack(spacing: 20) {
                    Spacer()
                    
                    TextField("First Name", text: $firstName)
                        .padding()
                        .background(Color.white.opacity(0.8))
                        .cornerRadius(10)
                        .padding(.horizontal, 40)
                    
                    TextField("Last Name", text: $lastName)
                        .padding()
                        .background(Color.white.opacity(0.8))
                        .cornerRadius(10)
                        .padding(.horizontal, 40)
                    
                    TextField("Email", text: $email)
                        .padding()
                        .background(Color.white.opacity(0.8))
                        .cornerRadius(10)
                        .padding(.horizontal, 40)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)

                    SecureField("Password", text: $password)
                        .padding()
                        .background(Color.white.opacity(0.8))
                        .cornerRadius(10)
                        .padding(.horizontal, 40)

                    Button(action: {
                        print("Register with email and password")
                        EmailAuthHandler.shared.registerWithEmail(email: email, password: password, firstName: firstName, lastName: lastName) { success, message in
                            self.registrationSuccess = success
                            self.alertMessage = message
                            self.showAlert = true
                        }
                    }) {
                        Text("Register")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue.opacity(0.9))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                            .padding(.horizontal, 40)
                    }

                    Button(action: {
                        // Navigate manually to Sign In screen
                        self.navigateToSignIn = true
                    }) {
                        Text("Already have an account? Sign In")
                            .foregroundColor(.blue)
                            .padding(.top, 10)
                    }
                    
                    Spacer()
                    
                    NavigationLink(destination: GoogleSignInView(), isActive: $navigateToSignIn) {
                        EmptyView()
                    }
                    .hidden()
                }
                .padding(.top, 50)
                .alert(isPresented: $showAlert) {
                    Alert(
                        title: Text(registrationSuccess ? "Registered Successfully" : "Failed to Register"),
                        message: Text(alertMessage),
                        dismissButton: .default(Text("OK"), action: {
                            if registrationSuccess {
                                navigateToSignIn = true
                            }
                        })
                    )
                }
            }
            .navigationBarBackButtonHidden(true) // Hide back button on register screen
        }
    }
}


#Preview {
    RegisterView()
}
