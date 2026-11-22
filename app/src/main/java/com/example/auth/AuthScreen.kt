package com.example.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.WorldwideLanguages
import com.example.data.repository.ThemePreferencesStore
import com.example.ui.components.WorldwideLanguageSheet
import com.example.ui.theme.NavyMidGradientLight
import com.example.ui.theme.NavyTopGradientLight
import kotlinx.coroutines.launch

private enum class AuthTabMode(val label: String) {
    SIGN_IN("Sign In"),
    CREATE_ACCOUNT("Create Account")
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    themeStore: ThemePreferencesStore? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val storedLanguageTag by (themeStore?.languageTagFlow
        ?: kotlinx.coroutines.flow.flowOf("en"))
        .collectAsStateWithLifecycle(initialValue = "en")
    val activeLanguage = remember(storedLanguageTag) {
        WorldwideLanguages.findByTag(storedLanguageTag)
    }

    var authMode by remember { mutableStateOf(AuthTabMode.SIGN_IN) }
    var profileNameInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSilent by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSilent = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSilent = false
            },
            scope = scope
        )
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val isLight = backgroundColor.luminance() > 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(
                if (isLight) {
                    Modifier.background(
                        Brush.verticalGradient(
                            0.0f to NavyTopGradientLight.copy(alpha = 0.72f),
                            0.35f to NavyMidGradientLight.copy(alpha = 0.45f),
                            0.75f to backgroundColor
                        )
                    )
                } else {
                    Modifier
                }
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Clean Minimal Brand & Language Block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F2EC),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(88.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bookshelf_logo),
                        contentDescription = "Favbooks logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Read, translate & listen to your books",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .clickable { showLanguageSheet = true }
                        .testTag("auth_language_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = "Select language",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = activeLanguage.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Clean Sign In / Create Account Card + Name Profile Input
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Segmented Pill Switch: Sign In | Create Account
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AuthTabMode.entries.forEach { mode ->
                            val selected = authMode == mode
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        authMode = mode
                                        errorMessage = null
                                    }
                                    .testTag("auth_tab_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                // Name Profile Input shown on Create Account / Sign Up
                AnimatedVisibility(visible = authMode == AuthTabMode.CREATE_ACCOUNT) {
                    OutlinedTextField(
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text("Profile Name") },
                        placeholder = { Text("Enter your display name") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_account_name_input")
                    )
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        val trimmedName = profileNameInput.trim()
                        if (authMode == AuthTabMode.CREATE_ACCOUNT && trimmedName.isEmpty()) {
                            errorMessage = "Please enter your Profile Name to create your account."
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            themeStore?.setPendingProfileName(
                                if (authMode == AuthTabMode.CREATE_ACCOUNT) trimmedName else null
                            )
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        }
                    },
                    enabled = !isLoading && !isCheckingSilent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading || isCheckingSilent) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isCheckingSilent) "Checking session…" else "Continuing…",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (authMode == AuthTabMode.CREATE_ACCOUNT) {
                                "Create Account with Google"
                            } else {
                                stringResource(R.string.sign_in_with_google)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = if (authMode == AuthTabMode.CREATE_ACCOUNT) {
                        "Creates your Favbooks Name Profile linked safely to Google"
                    } else {
                        "Sign in with Google to access your Favbooks library"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showLanguageSheet) {
        WorldwideLanguageSheet(
            selectedLanguageTag = activeLanguage.tag,
            onSelectLanguage = { chosen ->
                scope.launch {
                    themeStore?.setPreferredLanguageTag(chosen.tag)
                }
            },
            onDismiss = { showLanguageSheet = false }
        )
    }
}
