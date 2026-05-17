## Package: `io.wispforest.owo.ui.parsing`

### `class` IncompatibleUIModelException

> Describes an error that occurred because the UIModel provided to a method did not match the expectations set by said method. These expectations are most often expressed in terms of component classes, a violation of which will throw this exception
>

### `class` UIModel

> A model of a UI hierarchy parsed from an XML definition. You can use this to create a UI adapter for your screen with {@link #createAdapter(Class, Screen)} as well as expanding templates via {@link #expandTemplate(Class, String, Map)}
>

**Methods:**

- `UIModel load(InputStream stream)`
  > Load the UI model declared in the XML document encoded by the given input stream. Contrary to {@link #load(Path)}, this method throws if a parsing error occurs @param stream The input stream to decode and read @return The parsed UI model
- `OwoUIAdapter<T> createAdapter(Class<T> expectedRootComponentClass, Screen screen)`
  > Create a UI adapter which contains the component hierarchy declared by this UI model, attached to the given screen. <p> If there are components in your hierarchy you need to modify in code after the main hierarchy has been parsed, give them an id and look them up via {@link ParentUIComponent#childById(Class, String)} @param expectedRootComponentClass The class the created root component is expected to have.                                   Should this be violated, an exception is thrown. If there                                   are no specific expectations about the type of                                   root component to create, pass {@link UIComponent}
- `OwoUIAdapter<T> createAdapterWithoutScreen(int x, int y, int width, int height, Class<T> expectedRootComponentClass)`
  > Create a UI adapter which contains the component hierarchy declared by this UI model, without the context of a screen <p> If there are components in your hierarchy you need to modify in code after the main hierarchy has been parsed, give them an id and look them up via {@link ParentUIComponent#childById(Class, String)} @param expectedRootComponentClass The class the created root component is expected to have.                                   Should this be violated, an exception is thrown. If there                                   are no specific expectations about the type of                                   root component to create, pass {@link UIComponent}
- `T parseComponent("unchecked")`
  > Attempt to parse the given XMl element into a component, expanding any templates encountered. If the XML does not describe a valid component, a {@link UIModelParsingException} may be thrown @param expectedClass    The class the parsed component is expected to                         have. Should this be violated, an exception is                         thrown. If there are no specific expectations about                         the type of component to parse, pass {@link UIComponent} @param componentElement The XML element represented the                         component to parse. @return The parsed component
- `T expandTemplate("unchecked")`
  > Expand a template into a component, applying parameter mappings by invoking the given mapping function and creating template children using the given child supplier @param expectedClass     The class the expanded template is expected to                          have. Should this be violated, an exception is                          thrown. If there are no specific expectations about                          the type of component to create, pass {@link UIComponent} @param name              The name of the template to expand @param parameterSupplier The parameter mapping function to invoke                          for each parameter encountered in the template @param childSupplier     The template child mapping function to invoke                          for each template child the target template defines @return The expanded template parsed into a component
- `T expandTemplate(Class<T> expectedClass, String name, Map<String, String> parameters)`
  > Expand a template into a component, applying the given parameter mappings. If the template defines child elements, this method will most likely fail because parameters for those can only be provided in XML @param expectedClass The class the expanded template is expected to                      have. Should this be violated, an exception is                      thrown. If there are no specific expectations about                      the type of component to create, pass {@link UIComponent} @param name          The name of the template to expand @param parameters    The parameter mappings to apply while                      expanding the template @return The expanded template parsed into a component
- `T parseComponentTree(Class<T> expectedRootComponentClass)`
- `void applySubstitutions(Element template)`
- `void expandChildren(Element template)`
- `Function<T, S> cascadeIfNull(Function<T, S> first, Function<T, S> second)`

### `class` UIModelLoader

**Methods:**

- `void setHotReloadPath(Identifier modelId, @Nullable Path reloadPath)`
  > Set the path from which to attempt a hot reload when the UI model with the given identifier is requested through {@link #get(Identifier)}. <p> Call with a {@code null} path to clear
- `Set<Identifier> allLoadedModels()`
- `void onResourceManagerReload(ResourceManager manager)`
- `boolean hasCompletedInitialLoad()`

### `class` UIModelParsingException

> Describes an error that happened during instantiation of a UIModel, most commonly due to improperly formatted XML or XML which describes invalid values
>

### `class` UIParsing

> A utility class containing the component factory registry as well as some utility functions to ease model parsing
>

**Methods:**

- `void registerFactory(String componentTagName, Function<Element, UIComponent> factory)`
- `void registerFactory(Identifier componentId, Function<Element, UIComponent> factory)`
  > Register a factory used to create components from XML elements. Most factories will only consider the tag name of the element, but more context can be extracted from the passed element @param componentId The identifier under which to register the component,                    which (separated by a period instead of a colon) is used                    as the tag name for which this factory gets invoked @param factory     The factory to register
- `Function<Element, UIComponent> getFactory(Element element)`
  > Get the appropriate component factory for the given XML element. An exception is thrown if none is registered @param element The element representing the component to be parsed @return The matching factory @throws UIModelParsingException If there is no registered factory                                 capable of parsing the given element
- `List<T> allChildrenOfType("unchecked")`
  > Extract all children of the given element which match the expected type @param type The type of child nodes to extract @param <T>  The class to cast the extracted nodes to @return A list of all children of {@code element} which have a type of {@code type}
- `Map<String, Element> childElements(Element element)`
  > Extract all child elements of the given element into a map from tag name to element. An exception is thrown if a tag name appears twice @return All element children of {@code element} mapped from tag name to element @throws UIModelParsingException If two or more children share the same tag name
- `int parseSignedInt(Node node)`
  > Tries to interpret the text content of the given node as a signed integer @throws UIModelParsingException If the text content does not                                 represent a valid signed integer
- `int parseUnsignedInt(Node node)`
  > Tries to interpret the text content of the given node as an unsigned integer @throws UIModelParsingException If the text content does not                                 represent a valid unsigned integer
- `float parseFloat(Node node)`
  > Tries to interpret the text content of the given node as a floating-point number @throws UIModelParsingException If the text content does not                                 represent a valid floating point number
- `double parseDouble(Node node)`
  > Tries to interpret the text content of the given node as a double-precision floating-point number @throws UIModelParsingException If the text content does not                                 represent a valid floating point number
- `boolean parseBool(Node node)`
  > Interprets the text content of the given node as a boolean - more specifically this method returns {@code true} if and only if the text content equals {@code true}, without respecting letter case
- `Identifier parseIdentifier(Node node)`
  > Tries to interpret the text content of the given node as an identifier @throws UIModelParsingException If the text content does not                                 represent a valid identifier
- `Component parseText(Element element)`
  > Interprets the text content of the given element as text. If the {@code translate} attribute is set to {@code true}, the content is interpreted as a translation key - otherwise it is returned literally
- `Optional<T> get(Map<String, E> properties, String key, Function<E, T> parser)`
  > Parse the property indicated by {@code key} into an object of type {@code T} @param properties The map containing all available properties @param key        The key of the property to parse @param parser     The parsing function to use @param <T>        The type of object to parse @return An optional containing the parsed property, or an empty optional if the requested property was not contained in the given map
- `void apply(Map<String, E> properties, String key, Function<E, T> parser, Consumer<T> consumer)`
  > Parse the property indicated by {@code key} into an object of type {@code T} and apply the given function if it was present @param properties The map containing all available properties @param key        The key of the property to parse @param parser     The parsing function to use @param consumer   The function to apply if the property was present                   in the map and successfully parsed @param <T>        The type of object to parse
- `void expectAttributes(Element element, String... attributes)`
  > Verify that all the given attributes are present on the given element and throw if one is missing @param element    The element to verify @param attributes The attributes to verify
- `void expectChildren(Element element, Map<String, Element> children, String... expected)`
  > Verify that all the given elements are present as children of the given element and throw if one is missing @param element  The element to verify @param children The children of that element @param expected The expected child elements
- `int parseInt(Node node, boolean allowNegative)`

