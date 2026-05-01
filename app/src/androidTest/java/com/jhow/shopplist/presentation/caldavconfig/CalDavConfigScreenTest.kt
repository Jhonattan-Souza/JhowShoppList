package com.jhow.shopplist.presentation.caldavconfig

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalDavConfigScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialState_displaysFormFields() {
        composeRule.setContent {
            CalDavConfigScreen(
                uiState = CalDavConfigUiState(isLoading = false)
            )
        }

        composeRule.onNodeWithTag(CalDavConfigTestTags.SERVER_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(CalDavConfigTestTags.USERNAME_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(CalDavConfigTestTags.PASSWORD_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(CalDavConfigTestTags.LIST_NAME_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(CalDavConfigTestTags.SAVE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun typingInFields_invokesCallbacks() {
        var serverUrl = ""
        var username = ""
        var password = ""
        var listName = ""

        composeRule.setContent {
            var state by remember { mutableStateOf(CalDavConfigUiState(isLoading = false)) }
            
            CalDavConfigScreen(
                uiState = state,
                callbacks = CalDavConfigCallbacks(
                    onServerUrlChanged = { 
                        serverUrl = it
                        state = state.copy(serverUrl = it)
                    },
                    onUsernameChanged = { 
                        username = it
                        state = state.copy(username = it)
                    },
                    onPasswordChanged = { 
                        password = it
                        state = state.copy(password = it)
                    },
                    onListNameChanged = { 
                        listName = it
                        state = state.copy(listName = it)
                    }
                )
            )
        }

        composeRule.onNodeWithTag(CalDavConfigTestTags.SERVER_FIELD).performTextInput("https://dav.example.com")
        composeRule.onNodeWithTag(CalDavConfigTestTags.USERNAME_FIELD).performTextInput("jhow")
        composeRule.onNodeWithTag(CalDavConfigTestTags.PASSWORD_FIELD).performTextInput("secret")
        composeRule.onNodeWithTag(CalDavConfigTestTags.LIST_NAME_FIELD).performTextInput("Groceries")

        assertTrue(serverUrl == "https://dav.example.com")
        assertTrue(username == "jhow")
        assertTrue(password == "secret")
        assertTrue(listName == "Groceries")
    }

    @Test
    fun clickingSave_invokesCallback() {
        var saveClicked = false

        composeRule.setContent {
            CalDavConfigScreen(
                uiState = CalDavConfigUiState(isLoading = false),
                callbacks = CalDavConfigCallbacks(
                    onSaveClicked = { saveClicked = true }
                )
            )
        }

        composeRule.onNodeWithTag(CalDavConfigTestTags.SAVE_BUTTON).performClick()
        assertTrue(saveClicked)
    }

    @Test
    fun imeActions_moveFocusThroughFieldsAndSubmitOnDone() {
        var saveClicked = false

        composeRule.setContent {
            CalDavConfigScreen(
                uiState = CalDavConfigUiState(isLoading = false),
                callbacks = CalDavConfigCallbacks(
                    onSaveClicked = { saveClicked = true }
                )
            )
        }

        composeRule.onNodeWithTag(CalDavConfigTestTags.SERVER_FIELD).performClick()
        composeRule.onNodeWithTag(CalDavConfigTestTags.SERVER_FIELD).assertIsFocused()
        composeRule.onNodeWithTag(CalDavConfigTestTags.SERVER_FIELD).performImeAction()
        composeRule.onNodeWithTag(CalDavConfigTestTags.USERNAME_FIELD).assertIsFocused()

        composeRule.onNodeWithTag(CalDavConfigTestTags.USERNAME_FIELD).performImeAction()
        composeRule.onNodeWithTag(CalDavConfigTestTags.PASSWORD_FIELD).assertIsFocused()

        composeRule.onNodeWithTag(CalDavConfigTestTags.PASSWORD_FIELD).performImeAction()
        composeRule.onNodeWithTag(CalDavConfigTestTags.LIST_NAME_FIELD).assertIsFocused()

        composeRule.onNodeWithTag(CalDavConfigTestTags.LIST_NAME_FIELD).performImeAction()

        assertTrue(saveClicked)
    }

    @Test
    fun passwordVisibilityToggle_switchesBetweenShowAndHideStates() {
        composeRule.setContent {
            var state by remember { mutableStateOf(CalDavConfigUiState(isLoading = false)) }

            CalDavConfigScreen(
                uiState = state,
                callbacks = CalDavConfigCallbacks(
                    onPasswordChanged = {
                        state = state.copy(password = it)
                    }
                )
            )
        }

        composeRule.onNodeWithTag(CalDavConfigTestTags.PASSWORD_FIELD).performTextInput("secret")
        composeRule.onNodeWithContentDescription("Show password").assertIsDisplayed()

        composeRule.onNodeWithTag(CalDavConfigTestTags.PASSWORD_VISIBILITY_TOGGLE).performClick()

        composeRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
    }

    @Test
    fun isSavingState_disablesFieldsAndShowsProgress() {
        composeRule.setContent {
            CalDavConfigScreen(
                uiState = CalDavConfigUiState(isLoading = false, isSaving = true)
            )
        }

        composeRule.onNodeWithTag(CalDavConfigTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
        composeRule.onNodeWithTag(CalDavConfigTestTags.SAVE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun successState_showsSuccessScreenAndHandlesCallbacks() {
        var doneClicked = false
        var configureAnotherClicked = false

        composeRule.setContent {
            CalDavConfigScreen(
                uiState = CalDavConfigUiState(
                    isLoading = false,
                    isSaveSuccessful = true,
                    savedServerUrl = "https://dav.example.com"
                ),
                callbacks = CalDavConfigCallbacks(
                    onNavigateBack = { doneClicked = true },
                    onResetForm = { configureAnotherClicked = true }
                )
            )
        }

        composeRule.onNodeWithTag(CalDavConfigTestTags.SUCCESS_ICON).assertIsDisplayed()
        composeRule.onNodeWithTag(CalDavConfigTestTags.SUCCESS_HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithTag(CalDavConfigTestTags.SUCCESS_SUMMARY).assertIsDisplayed()

        composeRule.onNodeWithTag(CalDavConfigTestTags.DONE_BUTTON).performClick()
        assertTrue(doneClicked)

        composeRule.onNodeWithTag(CalDavConfigTestTags.CONFIGURE_ANOTHER_BUTTON).performClick()
        assertTrue(configureAnotherClicked)
    }
}
