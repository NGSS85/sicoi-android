package br.com.sicoi.mobile

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import br.com.sicoi.mobile.ui.login.LoginScreen
import br.com.sicoi.mobile.ui.login.SignupScreen
import br.com.sicoi.mobile.ui.modules.ModulesScreen
import br.com.sicoi.mobile.ui.osform.OSFormScreen
import br.com.sicoi.mobile.ui.technicians.TechniciansScreen
import br.com.sicoi.mobile.ui.workorders.TechnicianHistoryScreen
import br.com.sicoi.mobile.ui.workorders.WorkOrdersScreen
import br.com.sicoi.mobile.ui.workorders.PausedWorkOrdersScreen

/**
 * Grafo de navegação do SICOI Mobile.
 *
 * Rotas:
 * - login
 * - signup
 * - modules/{userName}
 * - technicians
 * - technicians_pin
 * - workorders/{technicianId}/{technicianName}
 * - osform/{workOrderId}/{technicianName}
 * - osform_requester/{workOrderId}/{technicianName}
 */
object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MODULES = "modules/{userName}"
    const val TECHNICIANS = "technicians"
    const val TECHNICIANS_PIN = "technicians_pin"
    const val WORK_ORDERS = "workorders/{technicianId}/{technicianName}"
    const val OS_FORM = "osform/{workOrderId}/{technicianName}"
    const val OS_FORM_REQUESTER = "osform_requester/{workOrderId}/{technicianName}"
    const val TECHNICIAN_HISTORY = "technician_history/{technicianName}"
    const val PAUSED_ORDERS = "paused_orders/{technicianName}"

    fun modules(userName: String) = "modules/${userName.encode()}"
    fun workOrders(technicianId: String, technicianName: String) =
        "workorders/${technicianId.encode()}/${technicianName.encode()}"
    fun osForm(workOrderId: String, technicianName: String) =
        "osform/${workOrderId.encode()}/${technicianName.encode()}"
    fun osFormRequester(workOrderId: String, technicianName: String) =
        "osform_requester/${workOrderId.encode()}/${technicianName.encode()}"
    fun technicianHistory(technicianName: String) =
        "technician_history/${technicianName.encode()}"
    fun pausedOrders(technicianName: String) =
        "paused_orders/${technicianName.encode()}"

    private fun String.encode() = java.net.URLEncoder.encode(this, "UTF-8")
}

@Composable
fun SicoiNavGraph(
    startDestination: String = Routes.LOGIN,
    onLogout: suspend () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Tela 1: Login ──────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // TODO: pegar nome real do usuário logado
                    navController.navigate(Routes.modules("Técnico")) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Routes.SIGNUP)
                }
            )
        }

        // ── Cadastro ───────────────────────────────────────────────────────
        composable(Routes.SIGNUP) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Tela 2: Seleção de Módulos ─────────────────────────────────────
        composable(
            route = Routes.MODULES,
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Técnico"
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

            ModulesScreen(
                userName = userName,
                onNavigateToMaintenance = {
                    navController.navigate(Routes.TECHNICIANS)
                },
                onLogout = {
                    coroutineScope.launch {
                        onLogout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )

        }


        // ── Tela 3: Seleção de Técnicos ───────────────────────────────────
        composable(Routes.TECHNICIANS) {
            TechniciansScreen(
                onNavigateBack = { navController.popBackStack() },
                onSelectTechnician = { techId, techName ->
                    navController.navigate(Routes.workOrders(techId, techName))
                },
                onNavigateToPinList = { requesterName ->
                    navController.navigate(Routes.osFormRequester("new", requesterName))
                }
            )
        }

        // ── Tela 4: Lista de O.S. em Aberto ───────────────────────────────────────
        composable(
            route = Routes.WORK_ORDERS,
            arguments = listOf(
                navArgument("technicianId")   { type = NavType.StringType },
                navArgument("technicianName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val techId   = backStackEntry.arguments?.getString("technicianId")   ?: ""
            val techName = backStackEntry.arguments?.getString("technicianName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""

            WorkOrdersScreen(
                technicianId   = techId,
                technicianName = techName,
                onNavigateBack = { navController.popBackStack() },
                onSelectWorkOrder = { osId ->
                    navController.navigate(Routes.osForm(osId, techName))
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.technicianHistory(techName))
                },
                onNavigateToPausedOrders = {
                    navController.navigate(Routes.pausedOrders(techName))
                }
            )
        }

        // ── Tela 4.5: Lista de O.S. Pausadas ───────────────────────────────────────
        composable(
            route = Routes.PAUSED_ORDERS,
            arguments = listOf(navArgument("technicianName") { type = NavType.StringType })
        ) { backStackEntry ->
            val techName = backStackEntry.arguments?.getString("technicianName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""

            PausedWorkOrdersScreen(
                technicianName = techName,
                onNavigateBack = { navController.popBackStack() },
                onSelectWorkOrder = { osId ->
                    navController.navigate(Routes.osForm(osId, techName))
                }
            )
        }

        // ── Tela 4.1: Histórico do Técnico ──────────────────────────────────
        composable(
            route = Routes.TECHNICIAN_HISTORY,
            arguments = listOf(
                navArgument("technicianName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val techName = backStackEntry.arguments?.getString("technicianName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            TechnicianHistoryScreen(
                technicianName = techName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Tela 5: Formulário da O.S. (Técnico) ─────────────────────────
        composable(
            route = Routes.OS_FORM,
            arguments = listOf(
                navArgument("workOrderId")    { type = NavType.StringType },
                navArgument("technicianName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val osId     = backStackEntry.arguments?.getString("workOrderId")     ?: ""
            val techName = backStackEntry.arguments?.getString("technicianName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""

            OSFormScreen(
                workOrderId    = osId,
                technicianName = techName,
                isRequesterMode = false,
                onNavigateBack = { navController.popBackStack() },
                onFinalized    = {
                    navController.popBackStack()
                }
            )
        }

        // ── Tela 5.1: Formulário da O.S. (Solicitante / Reduzido) ─────────
        composable(
            route = Routes.OS_FORM_REQUESTER,
            arguments = listOf(
                navArgument("workOrderId")    { type = NavType.StringType },
                navArgument("technicianName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val osId     = backStackEntry.arguments?.getString("workOrderId")     ?: "new"
            val techName = backStackEntry.arguments?.getString("technicianName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""

            OSFormScreen(
                workOrderId    = osId,
                technicianName = techName,
                isRequesterMode = true,
                onNavigateBack = { navController.popBackStack() },
                onFinalized    = {
                    navController.popBackStack()
                }
            )
        }
    }
}
