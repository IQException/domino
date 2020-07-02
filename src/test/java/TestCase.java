import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


/**
 * @author wang.wei
 * @since 2019/12/2
 */
public class TestCase {


    @Test
    public void testSerialize() {
/*        JacksonSerializer serializer = JacksonSerializer.DEFAULT;
        Instant instant = Instant.now();

        System.out.println(serializer.serialize(instant));
        System.out.println(instant.equals(serializer.deserialize(serializer.serialize(instant), Instant.class)));*/

        Duration duration = Duration.between(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS), LocalDateTime.now().plusHours(10).truncatedTo(ChronoUnit.DAYS));
        System.out.println(duration.getSeconds());
        System.out.println(duration.toMillis());
        System.out.println(duration.toMinutes());
        System.out.println(duration.toDays());
        System.out.println(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).toString());

    }
}
