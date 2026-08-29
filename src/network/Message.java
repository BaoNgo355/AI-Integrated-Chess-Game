package network;

public class Message {
    public enum Type {
        MOVE,
        COLOR,
        PING,
        PONG,
        RESIGN,
        DRAW_OFFER,
        DRAW_ACCEPT,
        DRAW_DECLINE,
        CHAT,
        UNKNOWN
    }

    private final Type type;
    private final String[] params;

    private Message(Type type, String[] params) {
        this.type = type;
        this.params = params;
    }

    public Type getType() { return type; }
    public String[] getParams() { return params; }

    public String serialize() {
        StringBuilder sb = new StringBuilder(type.name());
        for (String p : params) {
            sb.append(':').append(p);
        }
        return sb.toString();
    }

    public static Message parse(String raw) {
        if (raw == null || raw.isEmpty()) return new Message(Type.UNKNOWN, new String[0]);

        String[] parts = raw.split(":", -1);
        Type type;
        try {
            type = Type.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            type = Type.UNKNOWN;
        }

        String[] params = new String[parts.length - 1];
        System.arraycopy(parts, 1, params, 0, parts.length - 1);
        return new Message(type, params);
    }

    public static Message move(int fromCol, int fromRow, int toCol, int toRow) {
        return new Message(Type.MOVE, new String[]{
            String.valueOf(fromCol), String.valueOf(fromRow),
            String.valueOf(toCol), String.valueOf(toRow), "0"
        });
    }

    public static Message move(int fromCol, int fromRow, int toCol, int toRow, int promoType) {
        return new Message(Type.MOVE, new String[]{
            String.valueOf(fromCol), String.valueOf(fromRow),
            String.valueOf(toCol), String.valueOf(toRow),
            String.valueOf(promoType)
        });
    }

    public static Message color(String color) {
        return new Message(Type.COLOR, new String[]{color});
    }

    public static Message ping() {
        return new Message(Type.PING, new String[0]);
    }

    public static Message pong() {
        return new Message(Type.PONG, new String[0]);
    }

    public static Message resign() {
        return new Message(Type.RESIGN, new String[0]);
    }

    public static Message drawOffer() {
        return new Message(Type.DRAW_OFFER, new String[0]);
    }

    public static Message drawAccept() {
        return new Message(Type.DRAW_ACCEPT, new String[0]);
    }

    public static Message drawDecline() {
        return new Message(Type.DRAW_DECLINE, new String[0]);
    }

    public int getIntParam(int index) {
        if (index < 0 || index >= params.length) return 0;
        try { return Integer.parseInt(params[index]); } catch (NumberFormatException e) { return 0; }
    }

    public String getStringParam(int index) {
        if (index < 0 || index >= params.length) return "";
        return params[index];
    }
}
