package br.com.sicoi.mobile

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.sicoi.mobile.ui.login.LoginScreen
import br.com.sicoi.mobile.ui.login.SignupScreen
import br.com.sicoi.mobile.ui.modules.ModulesScreen
import br.com.sicoi.mobile.ui.technicians.TechniciansScreen
import br.com.sicoi.mobile.ui.workorders.PausedWorkOrdersScreen
import br.com.sicoi.mobile.ui.workorders.TechnicianHistoryScreen
import br.com.sicoi.mobile.ui.workorders.WorkOrdersScreen
import br.com.sicoi.mobile.ui.osform.OSFormScreen
import kotlinx.coroutines.launch

/**
 * DefiniÃ§Ã£o centralizada de rotas de navegaÃ§Ã£o do App SICOI Mobile
 */
object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MODULES = "modules/{userName}"
    const val TECHNICIANS = "technicians"
    const val WORK_ORDERS = "work_orders/{technicianId}/{technicianName}"
    const val PAUSED_ORDERS = "paused_orders/{technicianName}"
    const val TECHNICIAN_HISTORY = "technician_history/{technicianName}"
    const val OS_FORM = "os_form/{workOrderId}/{technicianName}"
    const val OS_FORM_REQUESTER = "os_form_requester/{workOrderId}/{technicianName}"

    fun modules(userName: String) =
        "modules/${java.net.URLEncoder.encode(userName, "UTF-8")}"

    fun workOrders(technicianId: String, technicianName: String) =
        "work_orders/$technicianId/${java.net.URLEncoder.encode(technicianName, "UTF-8")}"

    fun pausedOrders(technicianName: String) =
        "paused_orders/${java.net.URLEncoder.encode(technicianName, "UTF-8")}"

    fun technicianHistory(technicianName: String) =
        "technician_history/${java.net.URLEncoder.encode(technicianName, "UTF-8")}"

    fun osForm(workOrderId: String, technicianName: String) =
        "os_form/$workOrderId/${java.net.URLEncoder.encode(technicianName, "UTF-8")}"

    fun osFormRequester(workOrderId: String, technicianName: String) =
        "os_form_requester/$workOrderId/${java.net.URLEncoder.encode(technicianName, "UTF-8")}"
}

/**
 * Grafo de navegaÃ§Ã£o principal
 */
@Composable
fun SicoiNavGraph(
    startDestination: String = Routes.LOGIN,
    onLogout: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // â”€â”€ Login â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.modules("TÃ©cnico")) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Routes.SIGNUP)
                }
            )
        }

        // â”€â”€ Cadastro â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Tela 2: SeleÃ§Ã£o de MÃ³dulos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(
            route = Routes.MODULES,
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "TÃ©cnico"

            ModulesScreen(
                userName = userName,
                onNavigateToMaintenance = {
                    navController.navigate(Routes.TECHNICIANS)
                },
                onLogout = {
                    onLogout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // â”€â”€ Tela 3: SeleÃ§Ã£o de TÃ©cnicos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Tela 4: Lista de O.S. em Aberto â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Tela 4.5: Lista de O.S. Pausadas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Tela 4.1: HistÃ³rico do TÃ©cnico â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Tela 5: FormulÃ¡rio da O.S. (TÃ©cnico) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.technicianHistory(techName))
                },
                onNavigateToPausedOrders = {
                    navController.navigate(Routes.pausedOrders(techName))
                }
            )
        }

        // â”€â”€ Tela 5.1: FormulÃ¡rio da O.S. (Solicitante / Reduzido) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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