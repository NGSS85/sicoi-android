package br.com.sicoi.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.sicoi.mobile.data.repository.AuthRepository
import br.com.sicoi.mobile.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class PendingApproval(val message: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

sealed class SignupUiState {
    object Idle : SignupUiState()
    object Loading : SignupUiState()
    object Success : SignupUiState()
    data class Error(val message: String) : SignupUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signupState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    val signupState: StateFlow<SignupUiState> = _signupState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState.Error("Preencha todos os campos")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            _loginState.value = when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> LoginUiState.Success
                is AuthResult.Error   -> when (result.message) {
                    "PENDING"  -> LoginUiState.PendingApproval(
                        "Cadastro aguardando aprovação do administrador."
                    )
                    "REJECTED" -> LoginUiState.Error(
                        "Acesso negado. Seu cadastro foi rejeitado. Entre em contato com o administrador."
                    )
                    else -> LoginUiState.Error(result.message)
                }
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String, company: String) {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            _signupState.value = SignupUiState.Error("Preencha todos os campos obrigatórios")
            return
        }
        viewModelScope.launch {
            _signupState.value = SignupUiState.Loading
            _signupState.value = when (val result = authRepository.signUp(email, password, fullName, company)) {
                is AuthResult.Success -> SignupUiState.Success
                is AuthResult.Error   -> SignupUiState.Error(result.message)
            }
        }
    }

    fun resetLoginState() { _loginState.value = LoginUiState.Idle }
    fun resetSignupState() { _signupState.value = SignupUiState.Idle }
}
