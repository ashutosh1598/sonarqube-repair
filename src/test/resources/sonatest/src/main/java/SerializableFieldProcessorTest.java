import java.io.Serializable;

public class SerializableFieldProcessorTest implements Serializable
{
    private Unser uns;
    public static void doNothing()
    {
        int x = 500;
        int y = x*x+x;
    }
}

class Unser
{

}