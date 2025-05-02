//
//  GoogleSignInView.swift
//  RFOXIA
//
//  Created by Yasser Yasser on 22/04/2025.
//

import SwiftUI


struct GoogleSignInView: View {
    @Environment(\.presentationMode) var presentationMode

    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showAlert = false
    @State private var alertMessage = ""
    @State private var navigateToRegister = false
    @State private var navigateToHome = false

    var body: some View {
        NavigationStack {
            ZStack {
                VStack(spacing: 20) {
                    Spacer()
                    
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
                        print("sign in with email and password")
                        if email.isEmpty || password.isEmpty {
                            self.alertMessage = "Please fill all the fields."
                            self.navigateToHome = false
                            self.showAlert = true
                        }else{
                            EmailAuthHandler.shared.signInWithEmail(email: email, password: password) { success, message in
                                self.alertMessage = message
                                self.navigateToHome = true
                                if success {
                                    UserDefaults.standard.set(true, forKey: "isLogin")
                                } else {
                                    self.showAlert = true
                                    UserDefaults.standard.set(false, forKey: "isLogin")
                                }
                            }
                        }
                    }) {
                        Text("Sign in")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue.opacity(0.9))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                            .padding(.horizontal, 40)
                    }
                    
                    Text("OR")
                        .foregroundColor(.black)
                        .padding(.top, 10)

                    GoogleButton {
                        if let rootVC = UIApplication.shared.connectedScenes
                            .compactMap({ ($0 as? UIWindowScene)?.keyWindow })
                            .first?.rootViewController {
                            
                            GoogleAuthHandler.shared.signIn(presenting: rootVC) { success in
                                if success {
                                    UserDefaults.standard.set(true, forKey: "isLogin")
                                    self.navigateToHome = true
                                } else {
                                    UserDefaults.standard.set(false, forKey: "isLogin")
                                    self.alertMessage = "Google sign-in failed."
                                    self.showAlert = true
                                    self.navigateToHome = false
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 30)
                    .frame(height: 50)
                    .cornerRadius(12)

                    Button(action: {
                        self.navigateToRegister = true
                    }) {
                        Text("Don't have an account? Register")
                            .foregroundColor(.blue)
                            .padding(.top, 10)
                    }
                    
                    Spacer()
                    
                    NavigationLink(destination: RegisterView(), isActive: $navigateToRegister) {
                        EmptyView()
                    }
                    
                    NavigationLink(destination: DeviceListView(), isActive: $navigateToHome) {
                        EmptyView()
                    }
                    .hidden()
                }
                .padding(.top, 50)
                .alert(isPresented: $showAlert) {
                    Alert(
                        title: Text("Failed to Sign In"),
                        message: Text(alertMessage),
                        dismissButton: .default(Text("OK"), action: {
                        })
                    )
                }
            }
            .navigationBarBackButtonHidden(true)
        }
    }
}


#Preview {
    GoogleSignInView()
}
