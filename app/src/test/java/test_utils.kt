import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.helpers.Logger
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import java.io.InputStream

fun readResource(name: String): String {
    val classloader =
        Thread.currentThread().contextClassLoader!!
    val inputStream: InputStream = classloader.getResourceAsStream(name)
    return inputStream.bufferedReader().readText()
}

fun readResourceBytes(name: String): ByteArray {
    val classloader =
        Thread.currentThread().contextClassLoader!!
    val inputStream: InputStream = classloader.getResourceAsStream(name)
    return inputStream.use { it.readBytes() }
}

@Suppress("unused")
object KotestProjectConfig : AbstractProjectConfig() {
    override val isolationMode: IsolationMode
        get() = IsolationMode.InstancePerTest
    override val parallelism: Int
        get() = 1

    override fun beforeAll() {
        super.beforeAll()
        Logger.TEST_MODE = true
    }
}


class FakeTransactionRunner : ParticipantDbTransactionRunner {
    override suspend fun <R> withTransaction(block: suspend () -> R): R {
        return block()
    }
}