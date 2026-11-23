package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOut
import com.example.data.ai.BookAiService
import com.example.data.model.BookItem
import com.example.data.model.WorldwideLanguages
import com.example.data.pdf.PdfStorageManager
import com.example.data.repository.BookshelfRepository
import com.example.data.repository.ThemePreferencesStore
import com.example.ui.components.WorldwideLanguageSheet
import com.example.ui.theme.MidIceWashLight
import com.example.ui.theme.TopIceWashLight
import com.example.ui.viewmodel.BookshelfViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class LibraryTab(val labelRes: Int) {
    BOOKSHELF(R.string.nav_bookshelf),
    DASHBOARD(R.string.nav_dashboard),
    BOOKMARKS(R.string.nav_bookmarks),
    ARCHIVE(R.string.nav_archive)
}

private enum class BookOpenMode {
    READ,
    PLAY
}

private data class BookOpenSession(
    val book: BookItem,
    val mode: BookOpenMode,
    val initialPageOverride: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedLibraryRoot(
    currentUserId: String,
    themeStore: ThemePreferencesStore,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: BookshelfViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BookshelfViewModel(
                    repository = BookshelfRepository(db),
                    pdfStorage = PdfStorageManager(app),
                    themeStore = themeStore,
                    currentUserId = currentUserId,
                    aiService = BookAiService(app)
                )
            }
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val preferredLanguageTag by viewModel.preferredLanguageTag.collectAsStateWithLifecycle()
    val activeLanguage = remember(preferredLanguageTag) {
        WorldwideLanguages.findByTag(preferredLanguageTag)
    }

    var selectedTab by remember { mutableStateOf(LibraryTab.BOOKSHELF) }
    var activeBookSession by remember { mutableStateOf<BookOpenSession?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPdf(uri)
        }
    }

    val openPdfPicker: () -> Unit = {
        pdfPickerLauncher.launch(arrayOf("application/pdf"))
    }

    val openBookSession: (BookItem, BookOpenMode, Int?) -> Unit = { book, mode, pageOverride ->
        activeBookSession = BookOpenSession(
            book = book,
            mode = mode,
            initialPageOverride = pageOverride
        )
    }

    val currentSession = activeBookSession
    if (currentSession != null) {
        when (currentSession.mode) {
            BookOpenMode.READ -> {
                PdfReaderScreen(
                    book = currentSession.book,
                    initialPageOverride = currentSession.initialPageOverride,
                    viewModel = viewModel,
                    onClose = { activeBookSession = null },
                    onSwitchToPlay = { pageIdx ->
                        activeBookSession = currentSession.copy(
                            mode = BookOpenMode.PLAY,
                            initialPageOverride = pageIdx
                        )
                    },
                    onOpenLanguagePicker = { showLanguageSheet = true }
                )
            }
            BookOpenMode.PLAY -> {
                BookAudioPlayScreen(
                    book = currentSession.book,
                    initialPageOverride = currentSession.initialPageOverride,
                    viewModel = viewModel,
                    onSwitchToRead = { pageIdx ->
                        activeBookSession = currentSession.copy(
                            mode = BookOpenMode.READ,
                            initialPageOverride = pageIdx
                        )
                    },
                    onClose = { activeBookSession = null },
                    onOpenLanguagePicker = { showLanguageSheet = true }
                )
            }
        }
    } else {
        val baseBackground = MaterialTheme.colorScheme.background
        val isLight = baseBackground.luminance() > 0.5f
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(baseBackground)
                .then(
                    if (isLight) {
                        Modifier.background(
                            Brush.verticalGradient(
                                0.0f to TopIceWashLight.copy(alpha = 0.7f),
                                0.20f to MidIceWashLight.copy(alpha = 0.4f),
                                0.45f to baseBackground
                            )
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.statusBars,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (selectedTab != LibraryTab.BOOKSHELF) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (selectedTab) {
                                        LibraryTab.BOOKSHELF -> stringResource(R.string.app_name)
                                        LibraryTab.DASHBOARD -> "Reading Progress"
                                        LibraryTab.BOOKMARKS -> "Saved Bookmarks"
                                        LibraryTab.ARCHIVE -> "Archived Books"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showProfileSheet = true },
                                    modifier = Modifier.testTag("toolbar_profile_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Name Profile"
                                    )
                                }
                                LanguageBadgeChip(
                                    languageTag = activeLanguage.tag,
                                    onClick = { showLanguageSheet = true }
                                )
                                IconButton(
                                    onClick = { showSignOutDialog = true },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign out"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                },
                floatingActionButton = {
                    if (selectedTab == LibraryTab.BOOKSHELF && useNavigationRail) {
                        FloatingActionButton(
                            onClick = openPdfPicker,
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .testTag("upload_pdf_fab")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.upload_pdf))
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useNavigationRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                LibraryTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tabIcon(tab, selected),
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(tab.labelRes)) },
                                        modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        when (selectedTab) {
                            LibraryTab.BOOKSHELF -> BookshelfScreen(
                                viewModel = viewModel,
                                activeLanguageTag = activeLanguage.tag,
                                onOpenProfile = { showProfileSheet = true },
                                onOpenLanguagePicker = { showLanguageSheet = true },
                                onOpenSignOut = { showSignOutDialog = true },
                                onOpenProgressTab = { selectedTab = LibraryTab.DASHBOARD },
                                onOpenBookmarksTab = { selectedTab = LibraryTab.BOOKMARKS },
                                onOpenBookRead = { book -> openBookSession(book, BookOpenMode.READ, null) },
                                onOpenBookPlay = { book -> openBookSession(book, BookOpenMode.PLAY, null) },
                                onRequestUploadPdf = openPdfPicker,
                                modifier = Modifier.weight(1f)
                            )
                            LibraryTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onShowMessage = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                                onOpenLanguagePicker = { showLanguageSheet = true },
                                modifier = Modifier.weight(1f)
                            )
                            LibraryTab.BOOKMARKS -> BookmarksScreen(
                                viewModel = viewModel,
                                onOpenBookmark = { book, page -> openBookSession(book, BookOpenMode.READ, page) },
                                onShowMessage = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            LibraryTab.ARCHIVE -> ArchiveScreen(
                                viewModel = viewModel,
                                onOpenBookRead = { book -> openBookSession(book, BookOpenMode.READ, null) },
                                onOpenBookPlay = { book -> openBookSession(book, BookOpenMode.PLAY, null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Reference-Style Clean White Bottom Navigation Bar with Icons + Labels
                    if (!useNavigationRail) {
                        FloatingCapsuleNavBar(
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    if (showLanguageSheet) {
        WorldwideLanguageSheet(
            selectedLanguageTag = activeLanguage.tag,
            onSelectLanguage = { chosen ->
                viewModel.setPreferredLanguage(chosen.tag)
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showProfileSheet) {
        NameProfileSheet(
            currentName = profile?.displayName ?: "Reader",
            email = profile?.email,
            totalPagesRead = profile?.totalPagesRead?.toInt() ?: 0,
            streakDays = profile?.streakDays?.toInt() ?: 0,
            activeLanguageName = activeLanguage.displayName,
            onSaveName = { updatedName ->
                viewModel.updateDisplayName(updatedName)
                showProfileSheet = false
            },
            onOpenLanguagePicker = {
                showProfileSheet = false
                showLanguageSheet = true
            },
            onSignOut = {
                showProfileSheet = false
                showSignOutDialog = true
            },
            onDismiss = { showProfileSheet = false }
        )
    }

    if (showSignOutDialog) {
        val readerName = profile?.displayName ?: "your account"
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Sign out of $readerName? Your cloud progress, archive, and bookmarks remain safely saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        signOut(credentialManager, scope, onSignOutComplete = onSignedOut)
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FloatingCapsuleNavBar(
    selectedTab: LibraryTab,
    onSelectTab: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("floating_bottom_nav_capsule")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LibraryTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 4.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = tabIcon(tab, selected),
                            contentDescription = stringResource(tab.labelRes),
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageBadgeChip(
    languageTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .padding(end = 4.dp)
            .clickable(onClick = onClick)
            .testTag("toolbar_language_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Select worldwide language",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = languageTag.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun tabIcon(tab: LibraryTab, selected: Boolean) = when (tab) {
    LibraryTab.BOOKSHELF -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    LibraryTab.DASHBOARD -> if (selected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
    LibraryTab.BOOKMARKS -> if (selected) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks
    LibraryTab.ARCHIVE -> if (selected) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameProfileSheet(
    currentName: String,
    email: String?,
    totalPagesRead: Int,
    streakDays: Int,
    activeLanguageName: String,
    onSaveName: (String) -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    var editedName by remember(currentName) { mutableStateOf(currentName) }
    val initial = currentName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$totalPagesRead pages read • $streakDays day streak",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text("Profile Name") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_edit_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSaveName(editedName) },
                    enabled = editedName.trim().isNotEmpty(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_profile_name_button")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Name", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenLanguagePicker,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(activeLanguageName)
                }
            }

            TextButton(
                onClick = onSignOut,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out")
            }
        }
    }
}
