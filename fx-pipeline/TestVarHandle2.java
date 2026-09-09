import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TestVarHandle2 {
    public static void main(String[] args) {
        VarHandle INT_HANDLE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());
        ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
        try {
            INT_HANDLE.compareAndSet(buffer, 4096, 0, 1);
            System.out.println("No exception thrown for 4096.");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        try {
            INT_HANDLE.compareAndSet(buffer, 2048, 0, 1);
            System.out.println("2048 works. Value at byte offset 2048: " + buffer.getInt(2048) + ". Value at byte offset 2048*4: " + buffer.getInt(2048*4 - 4));
        } catch (Exception e) {
            System.out.println("Exception 2048: " + e);
        }
    }
}
