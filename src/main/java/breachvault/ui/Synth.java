package breachvault.ui;

import java.util.Random;

/**
 * Крошечный синтезатор: рисует звук прямо числами, без файлов.
 * Звук — это массив отсчётов от -1 до 1, по RATE штук на секунду.
 * Тон — синусоида (или квадрат, или «пила») нужной частоты, шум — случайные числа,
 * эхо — та же волна, подмешанная с задержкой.
 */
final class Synth {

    static final float RATE = 44_100f;

    enum Wave { SINE, SQUARE, SAW }

    private Synth() {
    }

    static double[] buffer(double ms) {
        return new double[samples(ms)];
    }

    static int samples(double ms) {
        return (int) (RATE * ms / 1000);
    }

    /**
     * Тон длиной ms, частота плавно идёт от fromHz к toHz.
     * Огибающая: быстрая атака (3 мс) и затухание — чтобы звук не «щёлкал» в начале и в конце.
     */
    static void tone(double[] out, double startMs, double ms, double fromHz, double toHz, Wave wave, double gain) {
        int start = samples(startMs);
        int length = samples(ms);
        double phase = 0;
        for (int i = 0; i < length && start + i < out.length; i++) {
            double t = (double) i / length; // 0 → 1 по ходу звука
            double hz = fromHz + (toHz - fromHz) * t;
            phase += hz / RATE;
            double cycle = phase - Math.floor(phase); // положение внутри периода, 0 → 1
            double value = switch (wave) {
                case SINE -> Math.sin(2 * Math.PI * cycle);
                case SQUARE -> cycle < 0.5 ? 1 : -1;
                case SAW -> 2 * cycle - 1;
            };
            out[start + i] += value * gain * envelope(i, length);
        }
    }

    /** Белый шум — для щелчков и цифрового «треска». */
    static void noise(double[] out, double startMs, double ms, double gain, Random random) {
        int start = samples(startMs);
        int length = samples(ms);
        for (int i = 0; i < length && start + i < out.length; i++) {
            out[start + i] += (random.nextDouble() * 2 - 1) * gain * envelope(i, length);
        }
    }

    /** «Заикание»: звук то есть, то нет — rateHz раз в секунду, на первых ms миллисекундах. */
    static void stutter(double[] out, double ms, double rateHz) {
        int length = Math.min(samples(ms), out.length);
        for (int i = 0; i < length; i++) {
            boolean on = ((int) (i / RATE * rateHz * 2)) % 2 == 0;
            if (!on) {
                out[i] *= 0.15;
            }
        }
    }

    /** Простое эхо: к каждому отсчёту добавляем отсчёт из прошлого, ослабленный в feedback раз. */
    static void echo(double[] out, double delayMs, double feedback) {
        int delay = samples(delayMs);
        for (int i = delay; i < out.length; i++) {
            out[i] += out[i - delay] * feedback;
        }
    }

    /**
     * Готовим к проигрыванию: подгоняем громкость так, чтобы самый громкий отсчёт был ровно peak,
     * и переводим в 16-битные числа (little-endian), как в WAV.
     */
    static byte[] toPcm16(double[] samples, double peak) {
        double max = 0;
        for (double s : samples) {
            max = Math.max(max, Math.abs(s));
        }
        double scale = max == 0 ? 0 : peak / max;
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            short value = (short) Math.round(samples[i] * scale * Short.MAX_VALUE);
            pcm[2 * i] = (byte) value;
            pcm[2 * i + 1] = (byte) (value >> 8);
        }
        return pcm;
    }

    /** Атака 3 мс, потом экспоненциальное затухание, и мягкий ноль в самом конце. */
    private static double envelope(int i, int length) {
        double attack = Math.min(1, i / (RATE * 0.003));
        double decay = Math.exp(-3.0 * i / length);
        double tail = Math.min(1, (length - i) / (RATE * 0.004));
        return attack * decay * tail;
    }
}
