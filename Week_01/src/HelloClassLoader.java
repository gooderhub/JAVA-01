import java.io.FileInputStream;
import java.lang.reflect.Method;

public class HelloClassLoader extends ClassLoader{
    public static void main(String[] args) {
        try {
            Class<?> helloClazz = new HelloClassLoader().findClass("Hello");
            Object helloObject = helloClazz.newInstance(); //创建对象
            Method[] methods = helloClazz.getDeclaredMethods();  // 获取本类中的所有方法
            for (Method method : methods) {
                method.invoke(helloObject);  // 执行方法
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<?> findClass(String name) {
        byte[] bytes = getProcessedCode("D:\\Hello.xlass");
        return defineClass(name, bytes, 0, bytes.length);
    }

    private byte[] getProcessedCode(String path) {
        try {
            FileInputStream fileInputStream = new FileInputStream(path);
            byte[] bytes = new byte[1024];
            byte[] result = new byte[0];
            int len;
            if ((len=fileInputStream.read(bytes)) != -1) {
                result = new byte[len];
                for (int i = 0; i < len; i++) {
                    result[i] = (byte) (255-bytes[i]);
                }
            }
            fileInputStream.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}
