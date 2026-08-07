public class TestCp {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.class.path"));
        try {
            Class.forName("net.openhft.chronicle.wire.ReadMarshallable");
            System.out.println("FOUND");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
