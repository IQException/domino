import com.google.common.base.Objects;

import java.util.Calendar;

/**
 * @author wang.wei
 * @since 2019/12/2
 */
public class Bean {
    Calendar instant ;

    public Calendar getInstant() {
        return instant;
    }

    public void setInstant(Calendar instant) {
        this.instant = instant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bean bean = (Bean) o;
        return Objects.equal(instant, bean.instant);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(instant);
    }
}
