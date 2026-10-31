package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EchoTest {
    @ParameterizedTest(name = "feedback {0}")
    @ValueSource(doubles = [1.0, 1.5])
    fun `an echo refuses a feedback of 1 or more`(feedback: Double) {
        // Given an echo that comes back as loud or louder would never die out

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Echo(delayBeats = 0.5, feedback = feedback, wet = 0.2) }
    }

    @Test
    fun `an echo refuses a negative feedback`() {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Echo(delayBeats = 0.5, feedback = -0.1, wet = 0.2) }
    }

    @ParameterizedTest(name = "wet {0}")
    @ValueSource(doubles = [-0.1, 1.1])
    fun `an echo refuses a part outside 0 and 1`(wet: Double) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Echo(delayBeats = 0.5, feedback = 0.2, wet = wet) }
    }

    @ParameterizedTest(name = "delay {0}")
    @ValueSource(doubles = [0.0, -0.5])
    fun `an echo refuses a delay of no beat`(delayBeats: Double) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Echo(delayBeats = delayBeats, feedback = 0.2, wet = 0.2) }
    }
}
