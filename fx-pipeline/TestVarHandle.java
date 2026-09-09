import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TestVarHandle {
    public static void main(String[] args) {
        VarHandle INT_HANDLE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());
        ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
        try {
            INT_HANDLE.compareAndSet(buffer, 4096, 0, 0);
            System.out.println("Success! Offset 4096 works.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
