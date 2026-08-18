package com.vidi.droidmahjong.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Lang { PT, EN }

class Localization(private val context: Context) {
    private val prefs = context.getSharedPreferences("droidmahjong-prefs", Context.MODE_PRIVATE)

    var lang by mutableStateOf(
        Lang.entries.firstOrNull { it.name == prefs.getString("lang", null) } ?: Lang.PT
    )
        private set

    fun toggle() {
        lang = if (lang == Lang.PT) Lang.EN else Lang.PT
        prefs.edit().putString("lang", lang.name).apply()
    }

    fun t(key: String): String = strings[lang]?.get(key) ?: key

    companion object {
        private val strings: Map<Lang, Map<String, String>> = mapOf(
            Lang.PT to mapOf(
                "tapToContinue" to "Toque para continuar",
                "developedBy" to "Desenvolvido por",

                "menuTag" to "MAHJONG SOLITÁRIO",
                "menuSubtitle" to "Combine pares. Limpe o tabuleiro. Relaxe.",
                "play" to "Jogar",
                "howToPlay" to "Como Jogar",
                "continueGame" to "Continuar Jogo",

                "difficultyLabel" to "DIFICULDADE",
                "difficultyEasy" to "Fácil",
                "difficultyMedium" to "Médio",
                "difficultyHard" to "Difícil",

                "back" to "Voltar",
                "time" to "Tempo",
                "moves" to "Jogadas",
                "score" to "Pontos",
                "left" to "Restantes",
                "hint" to "Dica",
                "shuffle" to "Baralhar",
                "undo" to "Desfazer",
                "restart" to "Reiniciar",
                "restartConfirm" to "Recomeçar este jogo? O progresso atual perde-se.",
                "confirmYes" to "Sim",
                "confirmNo" to "Cancelar",

                "winTitle" to "Vitória!",
                "winSubtitle" to "Limpaste o tabuleiro por completo.",
                "stuckTitle" to "Sem jogadas disponíveis",
                "stuckSubtitle" to "Já não há pares livres para combinar. Podes baralhar as fichas restantes ou desfazer a última jogada.",
                "playAgain" to "Jogar Novamente",
                "backToMenu" to "Voltar ao Menu",
                "finalTime" to "Tempo final",
                "finalMoves" to "Jogadas",
                "finalScore" to "Pontuação",
                "noHintsLeft" to "Sem mais dicas disponíveis neste jogo.",
                "noMoreUndo" to "Não há jogadas para desfazer.",
                "shuffleImpossible" to "Não foi possível encontrar um baralhar resolúvel. Tenta desfazer.",

                "leaderboardTitle" to "MELHOR RESULTADO NESTA DIFICULDADE",
                "bestTime" to "Melhor tempo",
                "bestMoves" to "Menos jogadas",
                "newRecordTime" to "🏆 Novo recorde de tempo",
                "newRecordMoves" to "🏆 Novo recorde de jogadas",

                "htpTitle" to "Como Jogar",
                "htpIntro" to "O Mahjong Solitário joga-se com 144 fichas empilhadas em pirâmide. O objetivo é remover todas as fichas do tabuleiro, combinando-as duas a duas.",
                "htpFreeTitle" to "1. A regra da ficha \"livre\"",
                "htpFreeBody" to "Só podes selecionar uma ficha se ela estiver livre: nada por cima dela, e pelo menos um dos lados (esquerdo ou direito) completamente desimpedido.",
                "htpCoveredLabel" to "Coberta",
                "htpCoveredDesc" to "Tem uma ficha por cima — não pode ser jogada.",
                "htpBlockedLabel" to "Bloqueada",
                "htpBlockedDesc" to "Livre de fichas por cima, mas presa dos dois lados — não pode ser jogada.",
                "htpFreeLabel" to "Livre",
                "htpFreeDesc" to "Sem nada por cima e com um dos lados aberto — pode ser jogada.",
                "htpMatchTitle" to "2. Como combinar fichas",
                "htpMatchBody" to "Toca em duas fichas livres do mesmo tipo para as remover. As Flores e as Estações são especiais: qualquer Flor combina com qualquer outra Flor, e qualquer Estação com qualquer outra Estação — não precisam de ser iguais.",
                "htpToolsTitle" to "3. Ferramentas de apoio",
                "htpHintBody" to "realça um par jogável no tabuleiro. Tens um número limitado por jogo.",
                "htpShuffleBody" to "reorganiza as fichas restantes, mantendo sempre uma solução possível, caso fiques sem jogadas.",
                "htpUndoBody" to "repõe o último par removido.",
                "htpCloseButton" to "Entendido",

                "traditionalMode" to "4 Jogadores (Riichi)",
                "playLocal" to "Jogar Localmente",
                "tradSetupTitle" to "4 JOGADORES — RIICHI",
                "tradSetupSubtitle" to "Escolhe quantos lugares são humanos — os restantes são preenchidos por bots.",
                "humanPlayers" to "Jogadores humanos",
                "startMatch" to "Começar Partida",
                "windE" to "Este", "windS" to "Sul", "windW" to "Oeste", "windN" to "Norte",
                "you" to "Tu", "bot" to "Bot",
                "wallLeft" to "Parede",
                "roundLabel" to "Ronda", "handLabel" to "Mão",
                "doraLabel" to "Dora", "pointsLabel" to "Pontos",
                "riichiBtn" to "Riichi", "tsumoBtn" to "Tsumo", "ronBtn" to "Ron",
                "ponBtn" to "Pon", "chiBtn" to "Chi", "kanBtn" to "Kan", "passBtn" to "Passar",
                "waitingOthers" to "A aguardar os outros jogadores…",
                "yourTurnDiscard" to "A tua vez — toca numa ficha para descartar.",
                "tsumoWinTitle" to "Tsumo!", "ronWinTitle" to "Ron!",
                "exhaustiveDrawTitle" to "Parede Esgotada",
                "tenpaiLabel" to "Em Tenpai", "notenLabel" to "Sem Tenpai",
                "yakuLabel" to "Yaku", "hanLabel" to "Han", "fuLabel" to "Fu",
                "totalPoints" to "Total",
                "nextHand" to "Próxima Mão",
                "matchEndTitle" to "Fim de Jogo",
                "finalStandings" to "Classificação final",
                "riichiSticksLabel" to "Palitos de riichi na mesa"
            ),
            Lang.EN to mapOf(
                "tapToContinue" to "Tap to continue",
                "developedBy" to "Developed by",

                "menuTag" to "MAHJONG SOLITAIRE",
                "menuSubtitle" to "Match pairs. Clear the board. Unwind.",
                "play" to "Play",
                "howToPlay" to "How to Play",
                "continueGame" to "Continue Game",

                "difficultyLabel" to "DIFFICULTY",
                "difficultyEasy" to "Easy",
                "difficultyMedium" to "Medium",
                "difficultyHard" to "Hard",

                "back" to "Back",
                "time" to "Time",
                "moves" to "Moves",
                "score" to "Score",
                "left" to "Left",
                "hint" to "Hint",
                "shuffle" to "Shuffle",
                "undo" to "Undo",
                "restart" to "Restart",
                "restartConfirm" to "Restart this game? Current progress will be lost.",
                "confirmYes" to "Yes",
                "confirmNo" to "Cancel",

                "winTitle" to "You Win!",
                "winSubtitle" to "You cleared the entire board.",
                "stuckTitle" to "No Moves Available",
                "stuckSubtitle" to "There are no free matching pairs left. You can shuffle the remaining tiles or undo the last move.",
                "playAgain" to "Play Again",
                "backToMenu" to "Back to Menu",
                "finalTime" to "Final time",
                "finalMoves" to "Moves",
                "finalScore" to "Score",
                "noHintsLeft" to "No more hints available this game.",
                "noMoreUndo" to "Nothing to undo.",
                "shuffleImpossible" to "Couldn't find a solvable shuffle. Try undo instead.",

                "leaderboardTitle" to "BEST RESULT ON THIS DIFFICULTY",
                "bestTime" to "Best time",
                "bestMoves" to "Fewest moves",
                "newRecordTime" to "🏆 New time record",
                "newRecordMoves" to "🏆 New moves record",

                "htpTitle" to "How to Play",
                "htpIntro" to "Mahjong Solitaire is played with 144 tiles stacked into a pyramid. The goal is to clear the whole board by matching tiles two at a time.",
                "htpFreeTitle" to "1. The \"free tile\" rule",
                "htpFreeBody" to "You can only select a tile if it is free: nothing on top of it, and at least one side (left or right) completely open.",
                "htpCoveredLabel" to "Covered",
                "htpCoveredDesc" to "Has a tile stacked on top — cannot be played.",
                "htpBlockedLabel" to "Blocked",
                "htpBlockedDesc" to "Nothing on top, but boxed in on both sides — cannot be played.",
                "htpFreeLabel" to "Free",
                "htpFreeDesc" to "Nothing on top and one side open — can be played.",
                "htpMatchTitle" to "2. Matching tiles",
                "htpMatchBody" to "Tap two free tiles of the same type to remove them. Flowers and Seasons are special: any Flower matches any other Flower, and any Season matches any other Season — they don't need to be identical.",
                "htpToolsTitle" to "3. Helper tools",
                "htpHintBody" to "highlights one playable pair on the board. You get a limited number per game.",
                "htpShuffleBody" to "rearranges the remaining tiles while always keeping a possible solution, in case you run out of moves.",
                "htpUndoBody" to "brings back the last pair you removed.",
                "htpCloseButton" to "Got it",

                "traditionalMode" to "4-Player (Riichi)",
                "playLocal" to "Play Locally",
                "tradSetupTitle" to "4-PLAYER — RIICHI",
                "tradSetupSubtitle" to "Choose how many seats are human — the rest are filled by bots.",
                "humanPlayers" to "Human players",
                "startMatch" to "Start Match",
                "windE" to "East", "windS" to "South", "windW" to "West", "windN" to "North",
                "you" to "You", "bot" to "Bot",
                "wallLeft" to "Wall",
                "roundLabel" to "Round", "handLabel" to "Hand",
                "doraLabel" to "Dora", "pointsLabel" to "Points",
                "riichiBtn" to "Riichi", "tsumoBtn" to "Tsumo", "ronBtn" to "Ron",
                "ponBtn" to "Pon", "chiBtn" to "Chi", "kanBtn" to "Kan", "passBtn" to "Pass",
                "waitingOthers" to "Waiting for other players…",
                "yourTurnDiscard" to "Your turn — tap a tile to discard.",
                "tsumoWinTitle" to "Tsumo!", "ronWinTitle" to "Ron!",
                "exhaustiveDrawTitle" to "Wall Exhausted",
                "tenpaiLabel" to "Tenpai", "notenLabel" to "Noten",
                "yakuLabel" to "Yaku", "hanLabel" to "Han", "fuLabel" to "Fu",
                "totalPoints" to "Total",
                "nextHand" to "Next Hand",
                "matchEndTitle" to "Match Over",
                "finalStandings" to "Final standings",
                "riichiSticksLabel" to "Riichi sticks on the table"
            )
        )
    }
}
