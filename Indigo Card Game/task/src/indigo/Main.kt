package indigo

import java.lang.RuntimeException

const val AMOUNT_PLAYER_CARD = 6
const val STAR_TABLE_CARD = 4
const val MAX_AMOUNT_CARDS = 52

enum class Suit(val symbol: String) {
    ORO("♦"),
    COPAS("♥"),
    ESPADA("♠"),
    PAUS("♣");

}

enum class Rank(val value: String) {
    A("A"),
    A2("2"),
    A3("3"),
    A4("4"),
    A5("5"),
    A6("6"),
    A7("7"),
    A8("8"),
    A9("9"),
    A10("10"),
    J("J"),
    Q("Q"),
    K("K");
}
val goldRanks = arrayOf(Rank.A, Rank.A10, Rank.J, Rank.Q, Rank.K)

data class Card(val rank: Rank, val suit: Suit) {
    override fun toString() = "${rank.value}${suit.symbol}"

    fun match(other: Card) = rank == other.rank || suit == other.suit
}


class Deck {
    private val cards = mutableListOf<Card>()

    init {
        for (s in Suit.values()) {
            for (r in Rank.values()) {
                cards.add(Card(rank = r, suit = s))
            }
        }
    }

    fun isEmpty() = cards.isEmpty()

    fun shuffle() {
        cards.shuffle()
    }

    fun pickCards(amount: Int): List<Card> {
        if (amount !in 1 .. MAX_AMOUNT_CARDS) {
            throw RuntimeException("Invalid number of cards.")

        }
        if (amount > cards.size) {
            throw RuntimeException("The remaining cards are insufficient to meet the request.")

        }

        val cardsTaken = cards.slice(0 until amount)
        cards.removeAll(cardsTaken)

        return cardsTaken
    }

}

class Game(val deck: Deck) {
    private var computer: Computer
    private var human: HumanPlayer
    private val lastCard: Card?
        get() = cardsOnTable.lastOrNull()
    private lateinit var firstPlayer: Player
    private lateinit var secondPlayer: Player
    lateinit var lastWon: Player
    private var cardsOnTable = mutableListOf<Card>()

    init {
        deck.shuffle()
        human = HumanPlayer(deck.pickCards(AMOUNT_PLAYER_CARD).toMutableList())
        computer = Computer(deck.pickCards(AMOUNT_PLAYER_CARD).toMutableList())

        cardsOnTable.addAll(deck.pickCards(STAR_TABLE_CARD))
    }

    fun defineHumanFirstThenComputer() {
        firstPlayer = human
        secondPlayer = computer
    }

    private fun addCard(card: Card) {
        cardsOnTable.add(card)
    }

    fun defineComputerFirstThenHuman() {
        firstPlayer = computer
        secondPlayer = human
    }

    private fun statusMessage(): String {
        val a = if (cardsOnTable.isEmpty()) "No" else "${cardsOnTable.size}"
        val b = if (cardsOnTable.isEmpty()) "" else ", and the top card is $lastCard"
        return "\n$a cards on the table$b"
    }

    fun machLastCardByRankSuit(card: Card): Boolean {
        return lastCard?.match(card) ?: false
    }

    fun resetCardOnTable() {
        cardsOnTable.clear()
    }

    fun winStatus(): String {
        val c = human.scoreWin
        val e = human.cardsWin
        val d = computer.scoreWin
        val f = computer.cardsWin
        return """
            Score: Player $c - Computer $d
            Cards: Player $e - Computer $f
        """.trimIndent()
    }

    fun applyFinalRules() {
        if (::lastWon.isInitialized) {
            lastWon.receiveWinCards(cardsOnTable)
        }

        if (human.cardsWin == computer.cardsWin) {
            firstPlayer.scoreWin += 3
        } else if (human.cardsWin > computer.cardsWin) {
            human.scoreWin += 3
        } else {
            computer.scoreWin += 3
        }
    }

    fun gameIsOver(): Boolean {
        return (firstPlayer.cardsWin + secondPlayer.cardsWin + cardsOnTable.size) == MAX_AMOUNT_CARDS
    }

    fun start() {
        try {
            val initialCards = cardsOnTable.joinToString(" ")
            println("Initial cards on the table: $initialCards")
            var cardThrowed: Card
            var counter = 0

            while (!gameIsOver()) {
                cardThrowed = firstPlayer.throwCard(statusMessage(), lastCard)
                if (machLastCardByRankSuit(cardThrowed)) {
                    firstPlayer.receiveWinCards(cardsOnTable.toMutableList().apply { add(cardThrowed) })
                    resetCardOnTable()
                    firstPlayer.printStatus(winStatus())
                    lastWon = firstPlayer
                } else {
                    addCard(cardThrowed)
                }

                cardThrowed = secondPlayer.throwCard(statusMessage(), lastCard)
                if (machLastCardByRankSuit(cardThrowed)) {
                    secondPlayer.receiveWinCards(cardsOnTable.toMutableList().apply { add(cardThrowed) })
                    resetCardOnTable()
                    secondPlayer.printStatus(winStatus())
                    lastWon = secondPlayer
                } else {
                    addCard(cardThrowed)
                }
                counter += 1
                if (counter % AMOUNT_PLAYER_CARD == 0 && !deck.isEmpty()) {
                    computer.fillCards(deck.pickCards(AMOUNT_PLAYER_CARD))
                    human.fillCards(deck.pickCards(AMOUNT_PLAYER_CARD))
                    counter = 0
                }

            }
            applyFinalRules()
            println(statusMessage())
            println(winStatus())
        } catch (_: InteruptGameException) {
        } finally {
            println("Game Over")
        }
    }
}

class InteruptGameException : RuntimeException()


interface Player {
    var scoreWin: Int
    var cardsWin: Int

    fun throwCard(status: String, topCard: Card?): Card
    fun receiveWinCards(winCards: List<Card>)
    fun printStatus(status: String)
}

class Computer(private val cards: MutableList<Card>) : Player {
    override var scoreWin: Int = 0
    override var cardsWin: Int = 0

    override fun printStatus(status: String) {
        println("Computer wins cards\n$status")
    }

    override fun throwCard(status: String, topCard: Card?): Card {
        println(status)
        val msg = cards.joinToString(separator = " ")
        println(msg)
        val card = chooseCard(topCard)
        println("Computer plays $card")
        cards.remove(card)
        return card
    }

    fun chooseCard(topCard: Card?): Card {
        if (cards.size == 1) {
            return cards.first()
        }
        if (topCard != null) {
            val candidates = cards.filter { it.match(topCard) }
            if (candidates.isEmpty()) {
                return selectWithNoCardOnTable()
            }
            if (candidates.size == 1) {
                return candidates.first()
            } else {
                val sameSuit = candidates.filter { it.suit == topCard.suit }
                if (sameSuit.size >= 2) {
                    return sameSuit.random()
                } else {
                    val sameRank = candidates.filter { it.rank == topCard.rank }
                    if (sameRank.size >= 2) {
                        return sameRank.random()
                    }
                }
                return candidates.random()
            }
        } else {
            // condition for no cards on table A
            return selectWithNoCardOnTable()
        }
    }

    private fun selectWithNoCardOnTable(): Card {
        val multiSuit = cards.groupBy { it.suit }.filter  { it.value.size > 1 }
        if (multiSuit.isNotEmpty()) {
            return multiSuit.values.first().first()
        }
        val mutRank = cards.groupBy { it.rank }.filter { it.value.size > 1 }
        if (mutRank.isNotEmpty()) {
            return mutRank.values.first().first()
        }
        return cards.random()
    }

    fun fillCards(newCards: List<Card>) {
        assert(cards.isEmpty())
        cards.addAll(newCards)
    }

    override fun receiveWinCards(winCards: List<Card>) {
        cardsWin += winCards.size
        for (c in winCards) {
            if (c.rank in goldRanks) {
                scoreWin += 1
            }
        }
    }

}

class HumanPlayer(private val cards: MutableList<Card>) : Player {
    override var cardsWin: Int = 0
    override var scoreWin: Int = 0

    override fun printStatus(status: String) {
        println("Player wins cards\n$status")
    }

    override fun throwCard(status: String, topCard: Card?): Card {
        assert(cards.size > 0) { println("Cards on hand can't be zero") }
        println(status)
        val card = chooseCard()
        cards.remove(card)

        return card
    }

    fun fillCards(newCards: List<Card>) {
        assert(cards.isEmpty())
        cards.addAll(newCards)
    }

    override fun receiveWinCards(winCards: List<Card>) {
        cardsWin += winCards.size
        for (c in winCards) {
            if (c.rank in goldRanks) {
                scoreWin += 1
            }
        }
    }

    private fun chooseCard(): Card {
        var c = 1
        val msg = cards.joinToString(separator = " ") {
            val r = "$c)$it"
            c++
            return@joinToString r
        }
        println("Cards in hand: $msg")
        println("Choose a card to play (1-${cards.size}):")

        var usrInput = readln()

        while (true) {
            if (usrInput == "exit") {
                throw InteruptGameException()
            }
            if (usrInput.toIntOrNull() in 1..cards.size) {
                return cards[usrInput.toInt() - 1]
            }

            println("Choose a card to play (1-${cards.size}):")
            usrInput = readln()
        }
    }

}
fun main() {
    val deck = Deck()
    val game = Game(deck)

    println("Indigo Card Game")

    println("Play first?")
    var wantToStart = readln()
    while (true) {
        when (wantToStart) {
            "yes" -> {
                game.defineHumanFirstThenComputer()
                break
            }
            "no" -> {
                game.defineComputerFirstThenHuman()
                break
            }
            else -> println("Play first?")
        }
        wantToStart = readln()
    }
    game.start()
}