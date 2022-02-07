## How it works ?

## Simple mapping

### Prerequisite

Let's create two POJO (source, destination).
```java
// Source class
public class City {
    private String name;
    private String postalCode;
    private String ways; // String comma-separated ways
    // getters and setters
}

// Destination class
public class CityDTO {
    private String name;
    private String postalCode;
    private List<String> ways;
    // getters and setters
}
```

### Create mapper

You need to declare a mapping object like below
```java
import fr.fezlight.Mapperz;

public final class CityMapper {
    // We have declared a mapping object with 
    // source object City and destination object CityDTO
    static Mapperz<City, CityDTO> mapDto = Mapperz
            .init(City.class, CityDTO.class);
}
```

### Add declaration to mapper

You have to add one declare() method for each field you have to map.
```java
import fr.fezlight.Mapperz;

public final class CityMapper {
    static Mapperz<City, CityDTO> mapDto = Mapperz
            .init(City.class, CityDTO.class)
            // Adding one declare to map name from City to name of CityDTO.
            // he will use the getters/setters methods.
            .declare(City::getName, CityDTO::setName)
            // Adding another one declare to map postalCode from City to postalCode of CityDTO.
            .declare(City::getPostalCode, CityDTO::setPostalCode);
}
```

### Add method to call our mapper

Let's create a method used to call Mapperz.
```java
import fr.fezlight.Mapperz;

public final class CityMapper {
    static Mapperz<City, CityDTO> mapDto = Mapperz
            .init(City.class, CityDTO.class)
            // Adding one declare to map name from City to name of CityDTO.
            // he will use the getters/setters methods.
            .declare(City::getName, CityDTO::setName)
            // Adding another one declare to map postalCode from City to postalCode of CityDTO.
            .declare(City::getPostalCode, CityDTO::setPostalCode);
    
    // Method used to call mapping method
    public static CityDTO toDto(City city) {
        return mapDto.map(city);
    }
}
```

### Testing

Let's test now !!
```java
class CityMapperTest {  
    @Test
    void givenCitySource_whenMap_thenSuccess(){
        City city = new City();
        city.setName("Paris");
        city.setPostalCode("75000");
        city.setWays("Rue de Bellechasse,Rue de Courcelles,Rue de Surène");
        
        CityDTO result = CityMapper.toDto(city);
        
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(city.getName());
        assertThat(result.getPostalCode()).isEqualTo(city.getPostalCode());
        assertThat(result.getWays()).isNull(); // See below why ways is null
    }
}
```

Hum.. it's weird ? Why my **ways** attribute is null. Like i said before when you want to map a field you have to use declare() method but here no declare method describe mapping between **ways** attributes.

## Advanced mapping

> When you want to map an object to another, you are facing difficulties when your value type change within process or your target object have a constructor with needed arguments. **Let's explore advanced mapping !**

### Add declaration to mapper

Let's try to map the **ways** attribute.
```java
import fr.fezlight.Mapperz;

public final class CityMapper {
    static Mapperz<City, CityDTO> mapDto = Mapperz
            .init(City.class, CityDTO.class)
            // Adding one declare to map name from City to name of CityDTO.
            // he will use the getters/setters methods.
            .declare(City::getName, CityDTO::setName)
            // Adding another one declare to map postalCode from City to postalCode of CityDTO.
            .declare(City::getPostalCode, CityDTO::setPostalCode)
            // Error (not the same type)
            .declare(City::getWays, CityDTO::setWays);
    
    // Method used to call mapping method
    public static CityDTO toDto(City city) {
        return mapDto.map(city);
    }
}
```

Here you see an error between **City::getWays** and **CityDTO::setWays** because the targeted attribute has not the same type as the source attribute.

### Add a formatter to declaration

```java
import fr.fezlight.Mapperz;
import java.util.stream.Stream;

public final class CityMapper {
    static Mapperz<City, CityDTO> mapDto = Mapperz
            .init(City.class, CityDTO.class)
            // Adding one declare to map name from City to name of CityDTO.
            // he will use the getters/setters methods.
            .declare(City::getName, CityDTO::setName)
            // Adding another one declare to map postalCode from City to postalCode of CityDTO.
            .declare(City::getPostalCode, CityDTO::setPostalCode)
            // We are using the formatter parameter (in this case assuming ways can't be null)
            .declare(City::getWays, CityDTO::setWays, (s) -> Stream.of(s.split(",")).collect(Collectors.toList()));

    // Method used to call mapping method
    public static CityDTO toDto(City city) {
        return mapDto.map(city);
    }
}
```

In that case, we split the string comma-separated and transform it to a list. If you want to apply a null-check on the source field you can use the java 9 syntax below or you can see [here](https://www.baeldung.com/java-null-safe-streams-from-collections) alternative.
```java
Stream.ofNullable(s).flatMap(s -> Stream.of(s.split(","))).collect(Collectors.toList())
```

### Add a constructor declaration to mapper

Now if you have a complex scenario where you declare a multi arguments constructor in output class.

Let's redefine our CityDTO class because a city always have a **name** and **postalCode**
```java
// Destination class
public class CityDTO {
    private String name;
    private String postalCode;
    private List<String> ways;
    
    public CityDTO(String name, String postalCode){
        this.name = name;
        this.postalCode = postalCode;
    }
    
    // getters and setters
}
```

With this new scheme, let's test our mapper 
```java
class CityMapperTest {  
    @Test
    void givenCitySource_whenMap_thenSuccess(){
        City city = new City();
        city.setName("Paris");
        city.setPostalCode("75000");
        city.setWays("Rue de Bellechasse,Rue de Courcelles,Rue de Surène");
        
        CityDTO result = CityMapper.toDto(city);
        
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(city.getName());
        assertThat(result.getPostalCode()).isEqualTo(city.getPostalCode());
        assertThat(result.getWays()).isEqualTo(Stream.of(city.getWays().split(",")).collect(Collectors.toList()));
    }
}
```

An error occurred because ```mapDto.map(city)``` try to instanciate a new CityDTO with no args constructor but anyone exists.

Let's modify our mapper and add method to pass constructor parameter. Remember, we need a **name** and **postalCode**

```java
import fr.fezlight.Mapperz;
import java.util.stream.Stream;

public final class CityMapper {
    static Mapperz<City, CityDTO> mapDto = Mapperz
            .init(City.class, CityDTO.class)
            .declareInConstructor(City::getName)
            .declareInConstructor(City::getPostalCode)
            .declare(City::getWays, CityDTO::setWays, (s) -> Stream.of(s.split(",")).collect(Collectors.toList()));

    // Method used to call mapping method
    public static CityDTO toDto(City city) {
        return mapDto.map(city);
    }
}
```

Here i'm asking Mapperz to use **City::getName** and give it as the first parameter and **City::getPostalCode** as the second parameter just by method chaining order. 

Now it works !
