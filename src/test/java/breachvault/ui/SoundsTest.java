package breachvault.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Звук не послушаешь в тесте, но можно проверить числа: длину, громкость и чистые края. */
class SoundsTest {

    @Test
    void everyCueIsShortQuietAndClean() {
        for (Sounds.Cue cue : Sounds.Cue.values()) {
            byte[] pcm = Sounds.render(cue);
            int samples = pcm.length / 2;
            double seconds = samples / Synth.RATE;

            int peak = 0;
            for (int i = 0; i < samples; i++) {
                peak = Math.max(peak, Math.abs(sample(pcm, i)));
            }

            assertTrue(seconds > 0.01 && seconds <= 1.0, cue + ": длина " + seconds + " с");
            assertTrue(peak <= 0.3 * Short.MAX_VALUE + 1, cue + ": слишком громко, пик " + peak);
            assertTrue(peak > 0.05 * Short.MAX_VALUE, cue + ": почти тишина, пик " + peak);
            // Первый и последний отсчёты около нуля — иначе в динамиках будет щелчок
            assertTrue(Math.abs(sample(pcm, 0)) < 0.02 * Short.MAX_VALUE, cue + ": щелчок в начале");
            assertTrue(Math.abs(sample(pcm, samples - 1)) < 0.02 * Short.MAX_VALUE, cue + ": щелчок в конце");
        }
    }

    @Test
    void peakIsExactlyWhatWeAskFor() {
        double[] wave = Synth.buffer(50);
        Synth.tone(wave, 0, 50, 1000, 1000, Synth.Wave.SINE, 5); // нарочно «слишком громко»

        byte[] pcm = Synth.toPcm16(wave, 0.2);

        int peak = 0;
        for (int i = 0; i < pcm.length / 2; i++) {
            peak = Math.max(peak, Math.abs(sample(pcm, i)));
        }
        assertTrue(Math.abs(peak - 0.2 * Short.MAX_VALUE) <= 1, "пик " + peak);
    }

    private static int sample(byte[] pcm, int i) {
        return (short) ((pcm[2 * i] & 0xFF) | (pcm[2 * i + 1] << 8));
    }
}
