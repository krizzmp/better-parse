
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import org.w3c.dom.HTMLInputElement
import kotlin.browser.document
import kotlin.browser.window

fun main() {
    window.onload = {
        val parseButton = document.getElementById("parse")!!
        val exprInput = document.getElementById("expr") as HTMLInputElement
        val result = document.getElementById("result")!!
        parseButton.addEventListener("click", {
            val expr = exprInput.value
            val parseResult = BooleanGrammar.tryParseToEnd(expr)

            val resultText = when (parseResult) {
                is Parsed -> parseResult.value.toString()
                is ErrorResult -> parseResult.toString()
            }

            result.textContent = resultText
        })
    }
}

sealed class BooleanExpression

object TRUE : BooleanExpression() {
    override fun toString() = "TRUE"
}

object FALSE : BooleanExpression() {
    override fun toString(): String = "FALSE"
}

data class Variable(val name: String) : BooleanExpression()
data class Not(val body: BooleanExpression) : BooleanExpression()
data class And(val left: BooleanExpression, val right: BooleanExpression) : BooleanExpression()
data class Or(val left: BooleanExpression, val right: BooleanExpression) : BooleanExpression()
data class Impl(val left: BooleanExpression, val right: BooleanExpression) : BooleanExpression()

private object BooleanGrammar : Grammar<BooleanExpression>() {
    val tru by token("true")
    val fal by token("false")
    val id by token("\\w+")
    val lpar by token("\\(")
    val rpar by token("\\)")
    val not by token("!")
    val and by token("&")
    val or by token("\\|")
    val impl by token("->")
    val ws by token("\\s+", ignore = true)

    val negation by -not * parser(this::term) map { Not(it) }
    val bracedExpression by -lpar * parser(this::implChain) * -rpar

    val term: Parser<BooleanExpression> by
    (tru asJust TRUE) or
            (fal asJust FALSE) or
            (id map { Variable(it.text) }) or
            negation or
            bracedExpression

    val andChain by leftAssociative(term, and) { a, _, b -> And(a, b) }
    val orChain by leftAssociative(andChain, or) { a, _, b -> Or(a, b) }
    val implChain by rightAssociative(orChain, impl) { a, _, b -> Impl(a, b) }

    override val rootParser by implChain
}