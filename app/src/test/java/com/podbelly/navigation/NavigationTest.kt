package com.podbelly.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) { Text("Home") }
                composable(Screen.Discover.route) { Text("Discover") }
                composable(Screen.Library.route) { Text("Library") }
                composable(Screen.Queue.route) { Text("Queue") }
                composable(Screen.Settings.route) { Text("Settings") }
                composable(Screen.Player.route) { Text("Player") }
                composable(
                    route = Screen.PodcastDetail.route,
                    arguments = listOf(
                        navArgument("podcastId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val podcastId = backStackEntry.arguments?.getLong("podcastId")
                    Text("Podcast Detail $podcastId")
                }
            }
        }
    }

    @Test
    fun `start destination is Home`() {
        composeTestRule.runOnIdle {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            assertEquals(Screen.Home.route, currentRoute)
        }
    }

    @Test
    fun `navigate to Discover tab`() {
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Discover.route)
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Discover.route, navController.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun `navigate to Library tab`() {
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Library.route)
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Library.route, navController.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun `navigate to Queue tab`() {
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Queue.route)
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Queue.route, navController.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun `navigate to Settings tab`() {
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Settings.route)
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Settings.route, navController.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun `navigate to PodcastDetail with podcastId argument`() {
        val podcastId = 42L
        composeTestRule.runOnIdle {
            navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
        }
        composeTestRule.runOnIdle {
            val currentEntry = navController.currentBackStackEntry
            assertNotNull(currentEntry)
            assertEquals(Screen.PodcastDetail.route, currentEntry?.destination?.route)
            assertEquals(podcastId, currentEntry?.arguments?.getLong("podcastId"))
        }
    }

    @Test
    fun `navigate to Player screen`() {
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Player.route)
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Player.route, navController.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun `popBackStack returns to previous destination`() {
        // Start at Home, navigate to Discover
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Discover.route)
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Discover.route, navController.currentBackStackEntry?.destination?.route)
        }

        // Pop back to Home
        composeTestRule.runOnIdle {
            navController.popBackStack()
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun `PodcastDetail route extracts podcastId correctly`() {
        assertEquals("podcast/99", Screen.PodcastDetail.createRoute(99L))
        assertEquals("podcast/0", Screen.PodcastDetail.createRoute(0L))
        assertEquals("podcast/123456789", Screen.PodcastDetail.createRoute(123456789L))
    }

    @Test
    fun `launchSingleTop prevents duplicate destinations`() {
        // Navigate to Discover with launchSingleTop
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Discover.route) {
                launchSingleTop = true
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Discover.route, navController.currentBackStackEntry?.destination?.route)
        }

        // Navigate to Discover again with launchSingleTop
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Discover.route) {
                launchSingleTop = true
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Discover.route, navController.currentBackStackEntry?.destination?.route)
        }

        // Pop back -- should go straight to Home (not another Discover entry)
        composeTestRule.runOnIdle {
            navController.popBackStack()
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun `navigate through all tabs and back to home`() {
        val tabs = listOf(
            Screen.Discover.route,
            Screen.Library.route,
            Screen.Queue.route,
            Screen.Settings.route
        )

        // Navigate through each tab
        for (tab in tabs) {
            composeTestRule.runOnIdle {
                navController.navigate(tab)
            }
            composeTestRule.runOnIdle {
                assertEquals(tab, navController.currentBackStackEntry?.destination?.route)
            }
        }

        // Navigate back to Home
        composeTestRule.runOnIdle {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) {
                    inclusive = true
                }
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
        }
    }
}
