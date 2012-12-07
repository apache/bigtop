package org.apache.bigtop.itest;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.bigtop.itest.Contract;
import org.apache.bigtop.itest.ParameterSetter;
import org.apache.bigtop.itest.Property;
import org.apache.bigtop.itest.Variable;

@Contract(
        properties = {
                @Property(name="foo.int1", type=Property.Type.INT, intValue=1000),
                @Property(name="foo.int2", type=Property.Type.INT),
                @Property(name="foo.bar1", type=Property.Type.STRING, defaultValue="xyz"),
                @Property(name="foo.bar2", type=Property.Type.STRING),
                @Property(name="foo.bool1", type=Property.Type.BOOLEAN),
                @Property(name="foo.bool2", type=Property.Type.BOOLEAN)
        },
        env = {
                @Variable(name="HOME"),
                @Variable(name="BIGTOP_UNLIKELY_FOO_ENV", required=false)
        }
)
public class TestContractJava {
    public static int foo_int1;
    public static int foo_int2;
    protected static String foo_bar1;
    protected static String foo_bar2;
    private static boolean foo_bool1;
    private static boolean foo_bool2;

    static String HOME;
    static String BIGTOP_UNLIKELY_FOO_ENV;

    @BeforeClass
    public static void setUp() throws ClassNotFoundException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        System.setProperty("foo.int2", "100");
        System.setProperty("foo.bool2", "true");

        ParameterSetter.setProperties(TestContractJava.class);
        ParameterSetter.setEnv(TestContractJava.class);
    }

    @Test
    public void testPropSettings() {
        assertEquals("checking the value of foo_int1 from default value",
                1000, foo_int1);
        assertEquals("checking the value of foo_int2 from foo.int2",
                100, foo_int2);
        assertEquals("checking the value of foo_bar1 from default value",
                "xyz", foo_bar1);
        assertEquals("checking the value of foo_bar2 from unset value",
                "", foo_bar2);
        assertEquals("checking the value of foo_bool1 from unset value",
                false, foo_bool1);
        assertEquals("checking the value of foo_bar2 from foo.bool2",
                true, foo_bool2);
    }

    @Test
    public void testEnvSettings() {
        assertEquals("checking the value of $HOME",
                System.getenv("HOME"), HOME);
        assertEquals("checking the value of $BIGTOP_UNLIKELY_FOO_ENV",
                null, BIGTOP_UNLIKELY_FOO_ENV);
    }
}