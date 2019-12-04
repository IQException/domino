import com.ctrip.train.tieyouflight.domino.support.JacksonSerializer;
import org.joda.time.Instant;
import org.junit.Test;


/**
 * @author wang.wei
 * @since 2019/12/2
 */
public class TestCase {


    @Test
    public void testSerialize() {
        JacksonSerializer serializer = JacksonSerializer.DEFAULT;
        Instant instant = Instant.now();

        System.out.println(serializer.serialize(instant));
        System.out.println(instant.equals(serializer.deserialize(serializer.serialize(instant), Instant.class)));
    }
}
