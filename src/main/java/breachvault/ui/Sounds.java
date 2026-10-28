package breachvault.ui;

import breachvault.ui.Synth.Wave;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

/**
 * Звуки интерфейса. Все синтезируются при запуске (см. Synth) и держатся готовыми в памяти —
 * тогда звук начинается сразу, без задержки на чтение файла.
 * Нет звуковой карты или она занята — программа просто работает молча.
 */
final class Sounds {

    enum Cue { HOVER, CLICK, PICK, OPEN, GRANTED, DENIED }

    private static final AudioFormat FORMAT = new AudioFormat(Synth.RATE, 16, 1, true, false);

    private static volatile Map<Cue, Clip> clips = Map.of(); // пока пусто — молчим

    private Sounds() {
    }

    /** Готовит звуки в фоне, чтобы не задерживать появление окна. */
    static void init() {
        Thread thread = new Thread(Sounds::load, "sounds-init");
        thread.setDaemon(true);
        thread.start();
    }

    static void play(Cue cue) {
        Clip clip = clips.get(cue);
        if (clip == null) {
            return;
        }
        clip.stop();             // если ещё звучит — обрываем
        clip.setFramePosition(0); // и начинаем сначала
        clip.start();
    }

    private static void load() {
        Map<Cue, Clip> ready = new EnumMap<>(Cue.class);
        try {
            for (Cue cue : Cue.values()) {
                byte[] pcm = render(cue);
                Clip clip = AudioSystem.getClip();
                clip.open(FORMAT, pcm, 0, pcm.length);
                ready.put(cue, clip);
            }
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            return; // без звука, но без падения
        }
        clips = ready; // публикуем готовую карту целиком — volatile делает её видимой потоку JavaFX
    }

    /** Рецепты звуков. peak — громкость самого громкого места (1.0 = максимум), все тихие. */
    static byte[] render(Cue cue) {
        Random random = new Random(cue.ordinal()); // один и тот же шум при каждом запуске
        double[] s;
        switch (cue) {
            case HOVER -> {
                // Едва слышный высокий тик
                s = Synth.buffer(25);
                Synth.tone(s, 0, 25, 2600, 2400, Wave.SINE, 1);
                return Synth.toPcm16(s, 0.10);
            }
            case CLICK -> {
                // Короткий цифровой блип с падением тона и щелчком в начале
                s = Synth.buffer(60);
                Synth.tone(s, 0, 50, 1400, 900, Wave.SQUARE, 0.6);
                Synth.noise(s, 0, 8, 0.5, random);
                return Synth.toPcm16(s, 0.16);
            }
            case PICK -> {
                // Двойной «чирп» данных: два коротких тона подряд
                s = Synth.buffer(90);
                Synth.noise(s, 0, 6, 0.3, random);
                Synth.tone(s, 0, 35, 1800, 1800, Wave.SINE, 1);
                Synth.tone(s, 38, 45, 2700, 2700, Wave.SINE, 0.8);
                return Synth.toPcm16(s, 0.18);
            }
            case OPEN -> {
                // Матрица загружается: восходящий свип, «пинг» в конце и эхо
                s = Synth.buffer(700);
                Synth.tone(s, 0, 320, 220, 880, Wave.SAW, 0.35);
                Synth.tone(s, 0, 320, 440, 1760, Wave.SINE, 0.5);
                Synth.noise(s, 0, 320, 0.05, random);
                Synth.tone(s, 320, 160, 1760, 1760, Wave.SINE, 0.7);
                Synth.echo(s, 95, 0.35);
                return Synth.toPcm16(s, 0.22);
            }
            case GRANTED -> {
                // Светлое восходящее арпеджио (ми — ля — ми октавой выше) с эхом
                s = Synth.buffer(1000);
                Synth.tone(s, 0, 140, 660, 660, Wave.SINE, 1);
                Synth.tone(s, 0, 140, 330, 330, Wave.SINE, 0.3);
                Synth.tone(s, 120, 140, 880, 880, Wave.SINE, 1);
                Synth.tone(s, 240, 380, 1320, 1320, Wave.SINE, 1);
                Synth.tone(s, 240, 380, 660, 660, Wave.SINE, 0.35);
                Synth.echo(s, 120, 0.35);
                return Synth.toPcm16(s, 0.28);
            }
            case DENIED -> {
                // Низкий нисходящий гул с «заиканием» и цифровым треском
                s = Synth.buffer(520);
                Synth.tone(s, 0, 480, 180, 90, Wave.SAW, 0.8);
                Synth.tone(s, 0, 480, 90, 45, Wave.SQUARE, 0.25);
                Synth.noise(s, 0, 480, 0.12, random);
                Synth.tone(s, 0, 110, 1200, 300, Wave.SQUARE, 0.25); // короткий «зап» сверху
                Synth.stutter(s, 230, 22);
                return Synth.toPcm16(s, 0.28);
            }
            default -> throw new IllegalArgumentException(cue.toString());
        }
    }
}
