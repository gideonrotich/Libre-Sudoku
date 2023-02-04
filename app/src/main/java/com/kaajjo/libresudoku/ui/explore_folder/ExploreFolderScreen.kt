package com.kaajjo.libresudoku.ui.explore_folder

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.ui.components.CustomModalBottomSheet
import com.kaajjo.libresudoku.ui.components.EmptyScreen
import com.kaajjo.libresudoku.ui.components.board.BoardPreview
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.time.toKotlinDuration

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun ExploreFolderScreen(
    viewModel: ExploreFolderViewModel,
    navigateBack: () -> Unit,
    navigatePlayGame: (Triple<Long, Boolean, Long>) -> Unit,
    navigateImportFromFile: (Pair<String, Long>) -> Unit,
    navigateEditGame: (Pair<Long, Long>) -> Unit,
    navigateCreateSudoku: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var deleteBoardDialog by rememberSaveable { mutableStateOf(false) }
    // used for a delete dialog when deleting
    var deleteBoardDialogBoard: SudokuBoard? by remember { mutableStateOf(null) }

    val folder by viewModel.folder.collectAsStateWithLifecycle(null)
    val games by viewModel.games.collectAsStateWithLifecycle(emptyMap())

    var contentUri by remember { mutableStateOf<Uri?>(null) }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            contentUri = it
        }
    )

    LaunchedEffect(contentUri) {
        contentUri?.let { uri ->
            folder?.let { folder ->
                navigateImportFromFile(Pair(uri.toString(), folder.uid))
            }
        }
    }

    BackHandler(viewModel.drawerState.isVisible) {
        coroutineScope.launch {
            viewModel.drawerState.hide()
        }
    }

    Scaffold(
        topBar = {
            AnimatedContent(viewModel.inSelectionMode) { inSelectionMode ->
                if (inSelectionMode) {
                    SelectionTopAppbar(
                        title = { Text(viewModel.selectedBoardsList.size.toString()) },
                        onCloseClick = { viewModel.inSelectionMode = false },
                        onClickDeleteSelected = { deleteBoardDialog = true },
                        onClickSelectAll = { viewModel.addAllToSelection(games.map { it.key }) }
                    )
                } else {
                    DefaultTopAppBar(
                        title = {
                            folder?.let {
                                Text(it.name)
                            }
                        },
                        navigateBack = navigateBack,
                        onImportMenuClick = {
                            coroutineScope.launch {
                                viewModel.drawerState.show()
                            }
                        }
                    )
                }
            }

        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            if (folder != null && games.isNotEmpty()) {
                var expandedGameUid by rememberSaveable { mutableStateOf(-1L) }
                LaunchedEffect(viewModel.inSelectionMode) {
                    expandedGameUid = -1L
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = games.toList(),
                        key = { it.first.uid }
                    ) { game ->
                        GameInFolderWidget(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            board = game.first.initialBoard,
                            difficulty = stringResource(game.first.difficulty.resName),
                            type = stringResource(game.first.type.resName),
                            gameId = game.first.uid,
                            savedGame = game.second,
                            expanded = expandedGameUid == game.first.uid,
                            selected = viewModel.selectedBoardsList.contains(game.first),
                            onClick = {
                                if (!viewModel.inSelectionMode) {
                                    expandedGameUid = if (expandedGameUid != game.first.uid) game.first.uid else -1L
                                } else {
                                    viewModel.addToSelection(game.first)
                                }
                            },
                            onLongClick = {
                                viewModel.inSelectionMode = true
                                viewModel.addToSelection(game.first)
                            },
                            onPlayClick = { viewModel.prepareSudokuToPlay(game.first) },
                            onEditClick = {
                                navigateEditGame(Pair(game.first.uid, folder!!.uid))
                            },
                            onDeleteClick = {
                                deleteBoardDialogBoard = game.first
                                deleteBoardDialog = true
                            }
                        )
                    }
                }
            } else if (folder != null) {
                EmptyScreen(stringResource(R.string.folder_empty_label))
            }
        }
    }

    LaunchedEffect(viewModel.readyToPlay, viewModel.gameUidToPlay) {
        if (viewModel.readyToPlay) {
            viewModel.gameUidToPlay?.let {
                navigatePlayGame(Triple(it, viewModel.isPlayedBefore, folder!!.uid))
                viewModel.readyToPlay = false
            }
        }
    }
    LaunchedEffect(viewModel.selectedBoardsList) {
        if (viewModel.selectedBoardsList.isEmpty()) {
            viewModel.inSelectionMode = false
        }
    }
    LaunchedEffect(viewModel.inSelectionMode) {
        if (!viewModel.inSelectionMode) {
            viewModel.selectedBoardsList = emptyList()
        }
    }

    if (deleteBoardDialog) {
        AlertDialog(
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.dialog_delete_selected)) },
            text = {
                Text(
                    text = pluralStringResource(
                        R.plurals.delete_selected_in_folder,
                        viewModel.selectedBoardsList.size,
                        if (deleteBoardDialogBoard != null) 1 else viewModel.selectedBoardsList.size
                    )
                )
            },
            onDismissRequest = { deleteBoardDialog = false },
            dismissButton = {
                TextButton(onClick = { deleteBoardDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (deleteBoardDialogBoard != null) {
                        deleteBoardDialogBoard?.let { gameToDelete ->
                            viewModel.deleteGame(gameToDelete)
                            deleteBoardDialogBoard = null
                        }
                    } else {
                        viewModel.deleteSelected()
                    }
                    deleteBoardDialog = false
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        )
    }

    CustomModalBottomSheet(
        drawerState = viewModel.drawerState,
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add to folder",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair("Create new", Icons.Outlined.Create),
                        Pair("Import from file", Icons.Outlined.NoteAdd)
                    ).forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    when (index) {
                                        0 -> {
                                            folder?.let {
                                                navigateCreateSudoku(it.uid)
                                            }
                                        }

                                        1 -> {
                                            openDocumentLauncher.launch(arrayOf("*/*"))
                                        }

                                        else -> {}
                                    }
                                    coroutineScope.launch {
                                        viewModel.drawerState.hide()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.second,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp)
                            )
                            Text(
                                text = item.first,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameInFolderWidget(
    board: String,
    difficulty: String,
    type: String,
    gameId: Long,
    savedGame: SavedGame?,
    expanded: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = { },
) {
    ElevatedCard(
        modifier = modifier
            .clip(CardDefaults.elevatedShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(clip = false),
                exit = fadeOut() + shrinkHorizontally(clip = false),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            Column {
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .size(130.dp)
                    ) {
                        BoardPreview(
                            size = sqrt(board.length.toFloat()).toInt(),
                            boardString = board
                        )
                    }
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                    ) {
                        Text("$difficulty $type")

                        if (savedGame != null) {
                            Text(
                                stringResource(
                                    R.string.saved_game_time,
                                    savedGame.timer.toKotlinDuration()
                                        .toComponents { minutes, seconds, _ ->
                                            String.format(" %02d:%02d", minutes, seconds)
                                        }
                                )
                            )
                        } else {
                            Text(stringResource(R.string.game_not_started))
                        }

                        Text(stringResource(R.string.game_id, gameId))

                        if (savedGame != null && savedGame.canContinue) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.large
                                    )
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.can_continue_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    modifier = Modifier.animateContentSize(),
                    enter = slideInVertically(tween(200)) + expandVertically(tween(200)),
                    exit = slideOutVertically(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        IconWithText(
                            imageVector = Icons.Rounded.PlayArrow,
                            text = stringResource(R.string.action_play),
                            onClick = onPlayClick
                        )
                        IconWithText(
                            imageVector = Icons.Rounded.Edit,
                            text = stringResource(R.string.action_edit),
                            onClick = onEditClick
                        )
                        IconWithText(
                            imageVector = Icons.Outlined.Delete,
                            text = stringResource(R.string.action_delete),
                            onClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconWithText(
    imageVector: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector, contentDescription = null)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTopAppBar(
    title: @Composable () -> Unit,
    navigateBack: () -> Unit,
    onImportMenuClick: () -> Unit
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = navigateBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_round_arrow_back_24),
                    contentDescription = null
                )
            }
        },
        actions = {
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null
                    )
                }
                MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.AddCircleOutline,
                                    contentDescription = null
                                )
                            },
                            text = {
                                Text(stringResource(R.string.explore_folder_add_sudoku))
                            },
                            onClick = {
                                onImportMenuClick()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppbar(
    title: @Composable () -> Unit,
    onCloseClick: () -> Unit,
    onClickDeleteSelected: () -> Unit,
    onClickSelectAll: () -> Unit
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Rounded.Close, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = onClickDeleteSelected) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null
                )
            }
            IconButton(onClick = onClickSelectAll) {
                Icon(
                    painterResource(R.drawable.ic_outline_select_all_24),
                    contentDescription = null
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    )
}