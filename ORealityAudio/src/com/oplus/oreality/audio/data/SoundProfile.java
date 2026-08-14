package com.oplus.oreality.audio.data;

public enum SoundProfile {
    SMART("Smart", "Applies the optimal sound mode according to your scenario.", 6),
    MOVIE("Movie", "Real surround sound and clearer voices.", 2),
    GAMING("Gaming", "Brings out hidden details in game sounds.", 3),
    MUSIC("Music", "Faithfully reproduces vocals and instruments.", 1);

    private final String title;
    private final String summary;
    private final int audioXValue;

    SoundProfile(String title, String summary, int audioXValue) {
        this.title = title;
        this.summary = summary;
        this.audioXValue = audioXValue;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public int getAudioXValue() {
        return audioXValue;
    }

    public static SoundProfile fromName(String name) {
        if (name == null) {
            return SMART;
        }

        for (SoundProfile profile : values()) {
            if (profile.name().equals(name)) {
                return profile;
            }
        }
        return SMART;
    }
}
