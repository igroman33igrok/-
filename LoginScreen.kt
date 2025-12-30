package com.example.shotacon.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onSuccess: () -> Unit, context: Context) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var showNoInternetDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Вход в Шотакон", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    // Валидация полей ввода
                    when {
                        email.isBlank() -> {
                            Toast.makeText(context, "Введите email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                            Toast.makeText(context, "Введите корректный email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        password.isBlank() -> {
                            Toast.makeText(context, "Введите пароль", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        password.length < 6 -> {
                            Toast.makeText(context, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        !isConnectedToInternet(context) -> {
                            showNoInternetDialog = true
                            return@Button
                        }
                    }

                    loading = true
                    scope.launch {
                        try {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    onSuccess()
                                }
                                .addOnFailureListener { loginError ->
                                    // Пробуем регистрацию только если ошибка входа не связана с сетью
                                    if (!isNetworkError(loginError)) {
                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnSuccessListener {
                                                val uid = auth.currentUser?.uid
                                                if (uid != null) {
                                                    if (!isConnectedToInternet(context)) {
                                                        showNoInternetDialog = true
                                                        loading = false
                                                        return@addOnSuccessListener
                                                    }

                                                    try {
                                                        firestore.collection("users").document(uid)
                                                            .set(
                                                                mapOf(
                                                                    "favorites" to listOf<String>(),
                                                                    "darkTheme" to false,
                                                                    "createdAt" to System.currentTimeMillis()
                                                                )
                                                            )
                                                            .addOnSuccessListener {
                                                                onSuccess()
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Toast.makeText(
                                                                    context,
                                                                    "Ошибка сохранения данных: ${e.localizedMessage ?: "Неизвестная ошибка"}",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                                loading = false
                                                            }
                                                    } catch (e: Exception) {
                                                        showNoInternetDialog = true
                                                        loading = false
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { registerError ->
                                                val errorMsg = when {
                                                    registerError.localizedMessage?.contains("email", ignoreCase = true) == true ->
                                                        "Неверный формат email"
                                                    registerError.localizedMessage?.contains("password", ignoreCase = true) == true ->
                                                        "Пароль слишком слабый (минимум 6 символов)"
                                                    registerError.localizedMessage?.contains("network", ignoreCase = true) == true ->
                                                        "Проблемы с интернетом"
                                                    else -> "Ошибка регистрации: ${registerError.localizedMessage ?: "Неизвестная ошибка"}"
                                                }
                                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                                loading = false
                                            }
                                    } else {
                                        Toast.makeText(context, "Проблемы с интернетом. Проверьте подключение.", Toast.LENGTH_LONG).show()
                                        loading = false
                                    }
                                }
                                .addOnCompleteListener {
                                    loading = false
                                }
                        } catch (e: Exception) {
                            showNoInternetDialog = true
                            loading = false
                        }
                    }
                },
                enabled = !loading
            ) {
                Text(if (loading) "Загрузка..." else "Войти / Зарегистрироваться")
            }
        }

        if (showNoInternetDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Нет подключения к интернету") },
                text = { Text("Регистрация или вход невозможны без подключения.") },
                confirmButton = {
                    TextButton(onClick = {
                        showNoInternetDialog = false
                    }) {
                        Text("ОК")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showNoInternetDialog = false
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }) {
                        Text("Выйти")
                    }
                }
            )
        }
    }
}

fun isConnectedToInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun isNetworkError(error: Exception): Boolean {
    val message = error.localizedMessage ?: ""
    return message.contains("network", ignoreCase = true) ||
           message.contains("unreachable", ignoreCase = true) ||
           message.contains("timeout", ignoreCase = true) ||
           message.contains("connection", ignoreCase = true)
}
