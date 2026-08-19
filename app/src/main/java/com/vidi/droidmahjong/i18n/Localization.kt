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
                "htpModeSolitaire" to "Solitário",
                "htpModeRiichi" to "Riichi (4 Jogadores)",
                "htpRiichiIntro" to "O modo 4 Jogadores é o Mahjong Riichi tradicional: cada jogador tem uma mão de fichas e o objetivo é ser o primeiro a completar uma mão vencedora — 4 conjuntos de 3 fichas mais um par (ou uma das mãos especiais). Joga-se localmente contra bots, em pass-and-play com 1 a 4 pessoas no mesmo dispositivo.",
                "htpRiichiTurnTitle" to "1. Como se joga um turno",
                "htpRiichiTurnBody" to "Em cada turno compras uma ficha e descartas outra. As fichas descartadas ficam visíveis para todos.",
                "htpRiichiHandTitle" to "2. Formar uma mão vencedora",
                "htpRiichiHandBody" to "Precisas de 4 conjuntos (sequências de 3 fichas seguidas do mesmo naipe, ou trincas/quadras da mesma ficha) mais um par para poderes declarar vitória (Tsumo se compraste a ficha, Ron se a apanhaste do descarte de outro jogador). Há também duas mãos especiais: Chiitoitsu (7 pares diferentes) e Kokushi Musou (as 13 fichas \"orfãs\" — terminais e honras — mais uma repetida).",
                "htpRiichiCallsTitle" to "3. Chamadas: Chi, Pon e Kan",
                "htpRiichiCallsBody" to "Podes reclamar a última ficha descartada por outro jogador: Chi (sequência, só ao jogador à tua esquerda), Pon (trinca, de qualquer jogador) ou Kan (quadra, de qualquer jogador). Reclamar torna essa parte da tua mão \"aberta\" (visível a todos) e avança o turno diretamente para ti.",
                "htpRiichiDeclareTitle" to "4. Declarar Riichi",
                "htpRiichiDeclareBody" to "Se a tua mão está completamente fechada (sem chamadas) e a um único passo da vitória (tenpai), podes declarar Riichi: apostas uma ficha e a tua mão fica bloqueada até venceres ou o jogo terminar, mas ganhas pontos extra de yaku e acesso à ura-dora se venceres.",
                "htpRiichiScoringTitle" to "5. Pontuação",
                "htpRiichiScoringBody" to "Cada mão vencedora soma han (pontos por combinações especiais chamadas yaku, como Riichi, Tanyao ou Pinfu) e fu (pontos de base pela composição da mão), que juntos determinam quantos pontos ganhas dos outros jogadores. As fichas de dora contam pontos extra sem serem um yaku por si só.",

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
                "htpModeSolitaire" to "Solitaire",
                "htpModeRiichi" to "Riichi (4 Players)",
                "htpRiichiIntro" to "The 4 Players mode is traditional Riichi Mahjong: each player holds a hand of tiles and the goal is to be the first to complete a winning hand — 4 sets of 3 tiles plus a pair (or one of the special hand shapes). It's played locally against bots, pass-and-play with 1-4 people on the same device.",
                "htpRiichiTurnTitle" to "1. Playing a turn",
                "htpRiichiTurnBody" to "Each turn you draw a tile and discard one. Discarded tiles stay visible to everyone.",
                "htpRiichiHandTitle" to "2. Forming a winning hand",
                "htpRiichiHandBody" to "You need 4 sets (three tiles in a row of the same suit, or three/four of a kind) plus one pair to declare a win (Tsumo if you drew the winning tile yourself, Ron if you claimed it from another player's discard). There are also two special hands: Chiitoitsu (7 distinct pairs) and Kokushi Musou (all 13 \"orphan\" tiles — terminals and honors — plus one duplicate).",
                "htpRiichiCallsTitle" to "3. Calls: Chi, Pon and Kan",
                "htpRiichiCallsBody" to "You can claim another player's most recent discard: Chi (a run, only from the player to your left), Pon (three of a kind, from anyone) or Kan (four of a kind, from anyone). Claiming makes that part of your hand \"open\" (visible to everyone) and passes the turn straight to you.",
                "htpRiichiDeclareTitle" to "4. Declaring Riichi",
                "htpRiichiDeclareBody" to "If your hand is fully closed (no calls) and one tile away from winning (tenpai), you can declare Riichi: you bet a stick and your hand locks in place until you win or the round ends, but you gain bonus yaku points and access to ura-dora if you do win.",
                "htpRiichiScoringTitle" to "5. Scoring",
                "htpRiichiScoringBody" to "Each winning hand adds up han (points from special combinations called yaku, like Riichi, Tanyao or Pinfu) and fu (base points from how the hand is built), which together determine how many points you collect from the other players. Dora tiles add bonus points without being a yaku on their own.",

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
