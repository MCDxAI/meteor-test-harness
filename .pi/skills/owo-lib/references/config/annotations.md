### `@RegexConstraint`
This type of constraint applies to `String` config options and allows values which match the given regular expression

Example:
```java
@RegexConstraint("[a-z]{1,10}")
public String aStringOption = "matched";
```

---

### `@RangeConstraint`
This type of constraint applies to numeric options and allows values which are between `min` and `max`, both *inclusive*

Example:
```java
@RangeConstraint(min = 10, max = 20)
public int anIntOption = 16;

@RangeConstraint(min = 5.5d, max = 11.3d)
public double aDoubleOption = 7.5;
```

---

### `@PredicateConstraint`
This type of constraint applies to any field and allows values which match the given predicate function. You declare said function as a `static` method on your config model class and pass its name into the annotation.

Example:
```java
@Config(...)
public class MyConfigModel {

    // we want to only allow lists of length 5
    @PredicateConstraint("predicateFunction")
    public List<String> someOption = new ArrayList<>(List.of("1", "2", "3", "4", "5"));

    // so we declare a predicate method
    public static boolean predicateFunction(List<String> list) {
        // and do the check in here
        // this could be arbitrarily complex code, but
        // we'll keep it simple for this demonstration
        return list.size() == 5;
    }

}
```

Most of owo-config's features are enabled via annotations. The ones which aren't covered in other articles already are outlined on this page.

---

### `@Expanded`

This annotation applies to fields which are either a nested object or some sort of `List<T>` and makes them start expanded in the config screen.

---

### `@Hook`

This annotation applies to any field and declares that a callback registrar should be generated. Refer to this handy example:

```java [Config Model]
@Config(name = "my-config", wrapperName = "MyConfig")
public class MyConfigModel {

    public int withoutHook = 0;

    @Hook
    public int withHook = 1;
}
```

```java [Generated Wrapper]
public class MyConfig extends ConfigWrapper<MyConfigModel> {
    ...

    public int withoutHook()
    public void withoutHook(int value)

    public int withHook()
    public void withHook(int value)
    public subscribeToWithHook(Consumer<Integer> subscriber)
}
```

---

### `@ExcludeFromScreen`

This annotation applies to any field and simply hides it from the config screen - this is useful if the option is either internal or has no support in the config screen and you want to suppress the warning.

---

### `@RestartRequired`

This annotation applies to any field and declares that the option it represents only actually applies after a restart of the game. The config screen indicates this and the player is explicitly notified when they close the screen.

The annotation also has a secondary effect when combined with `@Sync(Option.SyncMode.OVERRIDE_CLIENT)`. Because an option which only applies after restart cannot be dynamically overridden by the server, owo-config will not allow players to connect if their client's value does not match the server.

![config mismatch example](../../assets/owo/config-sync-error.png)

