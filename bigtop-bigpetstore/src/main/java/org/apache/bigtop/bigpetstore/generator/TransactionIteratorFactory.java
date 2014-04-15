/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.bigpetstore.generator;


import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import org.apache.bigtop.bigpetstore.util.Pair;
import org.apache.bigtop.bigpetstore.util.StringUtils;

/**
 * This class generates our data. Over time we will use it to embed bias which
 * can then be teased out, i.e. by clutstering/classifiers. For example:
 *
 * certain products <--> certain years or days
 *
 *
 */
public class TransactionIteratorFactory {

    /**
     * Each "state" has a pet store , with a certain "proportion" of the
     * transactions. In this case colorado represents the majority of the
     * transactions.
     */

    public static enum STATE {

        // Each product is separated with an _ for its base price.
        // That is just to make it easy to add new products.
        // Each state is associated with a relative probability.
        AZ(.1f, "dog-food_10", "cat-food_8", "leather-collar_25",
                "snake-bite ointment_30", "turtle-food_11"),
        AK(.1f,
                "dog-food_10", "cat-food_8", "fuzzy-collar_19",
                "antelope-caller_20", "salmon-bait_30"),
        CT(.1f, "dog-food_10",
                "cat-food_8", "fuzzy-collar_19", "turtle-pellets_5"),
        OK(.1f,
                "dog-food_10", "cat-food_8", "duck-caller_13",
                "rodent-cage_40", "hay-bail_5", "cow-dung_2"),
        CO(.1f,
                "dog-food_10", "cat-food_8", "choke-collar_15",
                "antelope snacks_30", "duck-caller_18"),
        CA(.3f, "dog-food_10",
                "cat-food_8", "fish-food_12", "organic-dog-food_16",
                "turtle-pellets_5"),
        NY(.2f, "dog-food_10", "cat-food_8", "steel-leash_20",
                "fish-food_20", "seal-spray_25");

        public static Random rand = new Random();
        public float probability;
        public String[] products;

        private STATE(float probability, String... products) {
            this.probability = probability;
            this.products = products;
        }

        public Pair<String, Integer> randProduct() {
            String product = products[rand.nextInt(products.length - 1)];
            String name = StringUtils.substringBefore(product, "_");
            Integer basePrice = Integer.parseInt(StringUtils.substringAfter(
                    product, "_"));
            return new Pair(name, basePrice);
        }

    }

    public static class KeyVal<K, V> {

        public final K key;
        public final V val;

        public KeyVal(K key, V val) {
            this.key = key;
            this.val = val;
        }
    }

    private Iterator<KeyVal<String, String>> dataIterator;

    Random r;

    public TransactionIteratorFactory(final int records, final STATE state) {

        /**
         * Random is seeded by STATE. This way similar names will be randomly
         * selected for states .
         */
        r = new Random(state.hashCode());

        if (records == 0) {
            throw new RuntimeException(
                    "Cant create a data iterator with no records (records==0) !");
        }

        this.dataIterator = new Iterator<KeyVal<String, String>>() {
            int trans_id = 1;

            @Override
            public boolean hasNext() {
                // TODO Auto-generated method stub
                return trans_id <= records;
            }

            int repeat = 0;
            String fname = randFirstName();
            String lname = randLastName();

            @Override
            public KeyVal<String, String> next() {
                /**
                 * Some customers come back for more :) We repeat a name up to
                 * ten times.
                 */
                if (repeat > 0)
                    repeat--;
                else {
                    fname = randFirstName();
                    lname = randLastName();
                    repeat = (int) (r.nextGaussian() * 10f);
                }
                String key, val;
                key = join(",", "BigPetStore", "storeCode_" + state.name(),
                        trans_id++ + "");
                Pair<String, Integer> product_price = state.randProduct();
                val = join(
                        ",",
                        fname,
                        lname,
                        getDate().toString(),
                        fudgePrice(product_price.getFirst(),
                                product_price.getSecond())
                                + "", product_price.getFirst()); // products are
                                                                 // biased by
                                                                 // state

                return new KeyVal<String, String>(key, val);
            }

            @Override
            public void remove() {
                // TODO Auto-generated method stub

            }

        };
    }

    /**
     * Add some decimals to the price;
     *
     * @param i
     * @return
     */
    public Float fudgePrice(String product, Integer i) {
        float f = (float) i;
        if (product.contains("dog")) {
            return i + .50f;
        }
        if (product.contains("cat")) {
            return i - .50f;
        }
        if (product.contains("fish")) {
            return i - .25f;
        } else
            return i + .10f;
    }

    static String join(String sep, String... strs) {
        if (strs.length == 0) {
            return "";
        } else if (strs.length == 1) {
            return strs[0];
        }
        String temp = strs[0]; // inefficient ... should probably use
                               // StringBuilder instead
        for (int i = 1; i < strs.length; i++) {
            temp += "," + strs[i];
        }
        return temp;
    }

    public Iterator<KeyVal<String, String>> getData() {
        return this.dataIterator;
    }

    private String randFirstName() {
        return FIRSTNAMES[this.r.nextInt(FIRSTNAMES.length - 1)].toLowerCase();
    }

    private String randLastName() {
        return LASTNAMES[this.r.nextInt(LASTNAMES.length - 1)].toLowerCase();
    }

    private Date getDate() {
        return new Date(this.r.nextInt());
    }

    private Integer getPrice() {
        return this.r.nextInt(MAX_PRICE);
    }

    public static final Integer MINUTES_IN_DAY = 60 * 24;
    public static final Integer MAX_PRICE = 10000;

    private static String[] FIRSTNAMES = { "Aaron", "Abby", "Abigail", "Adam",
            "Alan", "Albert", "Alex", "Alexandra", "Alexis", "Alice", "Alicia",
            "Alisha", "Alissa", "Allen", "Allison", "Alyssa", "Amanda",
            "Amber", "Amy", "Andrea", "Andrew", "Andy", "Angel", "Angela",
            "Angie", "Anita", "Ann", "Anna", "Annette", "Anthony", "Antonio",
            "April", "Arthur", "Ashley", "Audrey", "Austin", "Autumn", "Baby",
            "Barb", "Barbara", "Becky", "Benjamin", "Beth", "Bethany", "Betty",
            "Beverly", "Bill", "Billie", "Billy", "Blake", "Bob", "Bobbie",
            "Bobby", "Bonnie", "Brad", "Bradley", "Brady", "Brandi", "Brandon",
            "Brandy", "Breanna", "Brenda", "Brent", "Brett", "Brian",
            "Brianna", "Brittany", "Brooke", "Brooklyn", "Bruce", "Bryan",
            "Caleb", "Cameron", "Candy", "Carl", "Carla", "Carmen", "Carol",
            "Carolyn", "Carrie", "Casey", "Cassandra", "Catherine", "Cathy",
            "Chad", "Charlene", "Charles", "Charlie", "Charlotte", "Chase",
            "Chasity", "Chastity", "Chelsea", "Cheryl", "Chester", "Cheyenne",
            "Chris", "Christian", "Christina", "Christine", "Christoph",
            "Christopher", "Christy", "Chuck", "Cindy", "Clara", "Clarence",
            "Clayton", "Clifford", "Clint", "Cody", "Colton", "Connie",
            "Corey", "Cory", "Courtney", "Craig", "Crystal", "Curtis",
            "Cynthia", "Dakota", "Dale", "Dallas", "Dalton", "Dan", "Dana",
            "Daniel", "Danielle", "Danny", "Darla", "Darlene", "Darrell",
            "Darren", "Dave", "David", "Dawn", "Dean", "Deanna", "Debbie",
            "Deborah", "Debra", "Denise", "Dennis", "Derek", "Derrick",
            "Destiny", "Devin", "Diana", "Diane", "Dillon", "Dixie", "Dominic",
            "Don", "Donald", "Donna", "Donnie", "Doris", "Dorothy", "Doug",
            "Douglas", "Drew", "Duane", "Dustin", "Dusty", "Dylan", "Earl",
            "Ed", "Eddie", "Edward", "Elaine", "Elizabeth", "Ellen", "Emily",
            "Eric", "Erica", "Erika", "Erin", "Ernest", "Ethan", "Eugene",
            "Eva", "Evelyn", "Everett", "Faith", "Father", "Felicia", "Floyd",
            "Francis", "Frank", "Fred", "Gabriel", "Gage", "Gail", "Gary",
            "Gene", "George", "Gerald", "Gina", "Ginger", "Glen", "Glenn",
            "Gloria", "Grace", "Greg", "Gregory", "Haley", "Hannah", "Harley",
            "Harold", "Harry", "Heath", "Heather", "Heidi", "Helen", "Herbert",
            "Holly", "Hope", "Howard", "Hunter", "Ian", "Isaac", "Jack",
            "Jackie", "Jacob", "Jade", "Jake", "James", "Jamie", "Jan", "Jane",
            "Janet", "Janice", "Jared", "Jasmine", "Jason", "Jay", "Jean",
            "Jeannie", "Jeff", "Jeffery", "Jeffrey", "Jenna", "Jennifer",
            "Jenny", "Jeremiah", "Jeremy", "Jerry", "Jesse", "Jessica",
            "Jessie", "Jill", "Jim", "Jimmy", "Joann", "Joanne", "Jodi",
            "Jody", "Joe", "Joel", "Joey", "John", "Johnathan", "Johnny",
            "Jon", "Jonathan", "Jonathon", "Jordan", "Joseph", "Josh",
            "Joshua", "Joyce", "Juanita", "Judy", "Julia", "Julie", "Justin",
            "Kaitlyn", "Karen", "Katelyn", "Katherine", "Kathleen", "Kathryn",
            "Kathy", "Katie", "Katrina", "Kay", "Kayla", "Kaylee", "Keith",
            "Kelly", "Kelsey", "Ken", "Kendra", "Kenneth", "Kenny", "Kevin",
            "Kim", "Kimberly", "Kris", "Krista", "Kristen", "Kristin",
            "Kristina", "Kristy", "Kyle", "Kylie", "Lacey", "Laken", "Lance",
            "Larry", "Laura", "Lawrence", "Leah", "Lee", "Leonard", "Leroy",
            "Leslie", "Levi", "Lewis", "Linda", "Lindsay", "Lindsey", "Lisa",
            "Lloyd", "Logan", "Lois", "Loretta", "Lori", "Louis", "Lynn",
            "Madison", "Mandy", "Marcus", "Margaret", "Maria", "Mariah",
            "Marie", "Marilyn", "Marion", "Mark", "Marlene", "Marsha",
            "Martha", "Martin", "Marty", "Marvin", "Mary", "Mary ann", "Mason",
            "Matt", "Matthew", "Max", "Megan", "Melanie", "Melinda", "Melissa",
            "Melody", "Michael", "Michelle", "Mickey", "Mike", "Mindy",
            "Miranda", "Misty", "Mitchell", "Molly", "Monica", "Morgan",
            "Mother", "Myron", "Nancy", "Natasha", "Nathan", "Nicholas",
            "Nick", "Nicole", "Nina", "Noah", "Norma", "Norman", "Olivia",
            "Paige", "Pam", "Pamela", "Pat", "Patricia", "Patrick", "Patty",
            "Paul", "Paula", "Peggy", "Penny", "Pete", "Phillip", "Phyllis",
            "Rachael", "Rachel", "Ralph", "Randall", "Randi", "Randy", "Ray",
            "Raymond", "Rebecca", "Regina", "Renee", "Rex", "Rhonda",
            "Richard", "Rick", "Ricky", "Rita", "Rob", "Robbie", "Robert",
            "Roberta", "Robin", "Rochelle", "Rocky", "Rod", "Rodney", "Roger",
            "Ron", "Ronald", "Ronda", "Ronnie", "Rose", "Roxanne", "Roy",
            "Russ", "Russell", "Rusty", "Ruth", "Ryan", "Sabrina", "Sally",
            "Sam", "Samantha", "Samuel", "Sandra", "Sandy", "Sara", "Sarah",
            "Savannah", "Scott", "Sean", "Seth", "Shanda", "Shane", "Shanna",
            "Shannon", "Sharon", "Shaun", "Shawn", "Shawna", "Sheila",
            "Shelly", "Sher", "Sherri", "Sherry", "Shirley", "Sierra",
            "Skyler", "Stacey", "Stacy", "Stanley", "Stephanie", "Stephen",
            "Steve", "Steven", "Sue", "Summer", "Susan", "Sydney", "Tabatha",
            "Tabitha", "Tamara", "Tammy", "Tara", "Tasha", "Tashia", "Taylor",
            "Ted", "Teresa", "Terri", "Terry", "Tessa", "Thelma", "Theresa",
            "Thomas", "Tia", "Tiffany", "Tim", "Timmy", "Timothy", "Tina",
            "Todd", "Tom", "Tommy", "Toni", "Tony", "Tonya", "Tracey",
            "Tracie", "Tracy", "Travis", "Trent", "Trevor", "Trey", "Trisha",
            "Tristan", "Troy", "Tyler", "Tyrone", "Unborn", "Valerie",
            "Vanessa", "Vernon", "Veronica", "Vicki", "Vickie", "Vicky",
            "Victor", "Victoria", "Vincent", "Virginia", "Vivian", "Walter",
            "Wanda", "Wayne", "Wendy", "Wesley", "Whitney", "William",
            "Willie", "Wyatt", "Zachary" };

    public static String[] LASTNAMES = { "Abbott", "Acevedo", "Acosta",
            "Adams", "Adkins", "Aguilar", "Aguirre", "Albert", "Alexander",
            "Alford", "Allen", "Allison", "Alston", "Alvarado", "Alvarez",
            "Anderson", "Andrews", "Anthony", "Armstrong", "Arnold", "Ashley",
            "Atkins", "Atkinson", "Austin", "Avery", "Avila", "Ayala", "Ayers",
            "Bailey", "Baird", "Baker", "Baldwin", "Ball", "Ballard", "Banks",
            "Barber", "Smith", "Johnson", "Williams", "Jones", "Brown",
            "Davis", "Miller", "Wilson", "Moore", "Taylor", "Thomas",
            "Jackson", "Barker", "Barlow", "Barnes", "Barnett", "Barr",
            "Barrera", "Barrett", "Barron", "Barry", "Bartlett", "Barton",
            "Bass", "Bates", "Battle", "Bauer", "Baxter", "Beach", "Bean",
            "Beard", "Beasley", "Beck", "Becker", "Bell", "Bender", "Benjamin",
            "Bennett", "Benson", "Bentley", "Benton", "Berg", "Berger",
            "Bernard", "Berry", "Best", "Bird", "Bishop", "Black", "Blackburn",
            "Blackwell", "Blair", "Blake", "Blanchard", "Blankenship",
            "Blevins", "Bolton", "Bond", "Bonner", "Booker", "Boone", "Booth",
            "Bowen", "Bowers", "Bowman", "Boyd", "Boyer", "Boyle", "Bradford",
            "Bradley", "Bradshaw", "Brady", "Branch", "Bray", "Brennan",
            "Brewer", "Bridges", "Briggs", "Bright", "Britt", "Brock",
            "Brooks", "Browning", "Bruce", "Bryan", "Bryant", "Buchanan",
            "Buck", "Buckley", "Buckner", "Bullock", "Burch", "Burgess",
            "Burke", "Burks", "Burnett", "Burns", "Burris", "Burt", "Burton",
            "Bush", "Butler", "Byers", "Byrd", "Cabrera", "Cain", "Calderon",
            "Caldwell", "Calhoun", "Callahan", "Camacho", "Cameron",
            "Campbell", "Campos", "Cannon", "Cantrell", "Cantu", "Cardenas",
            "Carey", "Carlson", "Carney", "Carpenter", "Carr", "Carrillo",
            "Carroll", "Carson", "Carter", "Carver", "Case", "Casey", "Cash",
            "Castaneda", "Castillo", "Castro", "Cervantes", "Chambers", "Chan",
            "Chandler", "Chaney", "Chang", "Chapman", "Charles", "Chase",
            "Chavez", "Chen", "Cherry", "Christensen", "Christian", "Church",
            "Clark", "Clarke", "Clay", "Clayton", "Clements", "Clemons",
            "Cleveland", "Cline", "Cobb", "Cochran", "Coffey", "Cohen", "Cole",
            "Coleman", "Collier", "Collins", "Colon", "Combs", "Compton",
            "Conley", "Conner", "Conrad", "Contreras", "Conway", "Cook",
            "Cooke", "Cooley", "Cooper", "Copeland", "Cortez", "Cote",
            "Cotton", "Cox", "Craft", "Craig", "Crane", "Crawford", "Crosby",
            "Cross", "Cruz", "Cummings", "Cunningham", "Curry", "Curtis",
            "Dale", "Dalton", "Daniel", "Daniels", "Daugherty", "Davenport",
            "David", "Davidson", "Dawson", "Day", "Dean", "Decker", "Dejesus",
            "Delacruz", "Delaney", "Deleon", "Delgado", "Dennis", "Diaz",
            "Dickerson", "Dickinson", "Dillard", "Dillon", "Dixon", "Dodson",
            "Dominguez", "Donaldson", "Donovan", "Dorsey", "Dotson", "Douglas",
            "Downs", "Doyle", "Drake", "Dudley", "Duffy", "Duke", "Duncan",
            "Dunlap", "Dunn", "Duran", "Durham", "Dyer", "Eaton", "Edwards",
            "Elliott", "Ellis", "Ellison", "Emerson", "England", "English",
            "Erickson", "Espinoza", "Estes", "Estrada", "Evans", "Everett",
            "Ewing", "Farley", "Farmer", "Farrell", "Faulkner", "Ferguson",
            "Fernandez", "Ferrell", "Fields", "Figueroa", "Finch", "Finley",
            "Fischer", "Fisher", "Fitzgerald", "Fitzpatrick", "Fleming",
            "Fletcher", "Flores", "Flowers", "Floyd", "Flynn", "Foley",
            "Forbes", "Ford", "Foreman", "Foster", "Fowler", "Fox", "Francis",
            "Franco", "Frank", "Franklin", "Franks", "Frazier", "Frederick",
            "Freeman", "French", "Frost", "Fry", "Frye", "Fuentes", "Fuller",
            "Fulton", "Gaines", "Gallagher", "Gallegos", "Galloway", "Gamble",
            "Garcia", "Gardner", "Garner", "Garrett", "Garrison", "Garza",
            "Gates", "Gay", "Gentry", "George", "Gibbs", "Gibson", "Gilbert",
            "Giles", "Gill", "Gillespie", "Gilliam", "Gilmore", "Glass",
            "Glenn", "Glover", "Goff", "Golden", "Gomez", "Gonzales",
            "Gonzalez", "Good", "Goodman", "Goodwin", "Gordon", "Gould",
            "Graham", "Grant", "Graves", "Gray", "Green", "Greene", "Greer",
            "Gregory", "Griffin", "Griffith", "Grimes", "Gross", "Guerra",
            "Guerrero", "Guthrie", "Gutierrez", "Guy", "Guzman", "Hahn",
            "Hale", "Haley", "Hall", "Hamilton", "Hammond", "Hampton",
            "Hancock", "Haney", "Hansen", "Hanson", "Hardin", "Harding",
            "Hardy", "Harmon", "Harper", "Harris", "Harrington", "Harrison",
            "Hart", "Hartman", "Harvey", "Hatfield", "Hawkins", "Hayden",
            "Hayes", "Haynes", "Hays", "Head", "Heath", "Hebert", "Henderson",
            "Hendricks", "Hendrix", "Henry", "Hensley", "Henson", "Herman",
            "Hernandez", "Herrera", "Herring", "Hess", "Hester", "Hewitt",
            "Hickman", "Hicks", "Higgins", "Hill", "Hines", "Hinton", "Hobbs",
            "Hodge", "Hodges", "Hoffman", "Hogan", "Holcomb", "Holden",
            "Holder", "Holland", "Holloway", "Holman", "Holmes", "Holt",
            "Hood", "Hooper", "Hoover", "Hopkins", "Hopper", "Horn", "Horne",
            "Horton", "House", "Houston", "Howard", "Howe", "Howell",
            "Hubbard", "Huber", "Hudson", "Huff", "Huffman", "Hughes", "Hull",
            "Humphrey", "Hunt", "Hunter", "Hurley", "Hurst", "Hutchinson",
            "Hyde", "Ingram", "Irwin", "Jacobs", "Jacobson", "James", "Jarvis",
            "Jefferson", "Jenkins", "Jennings", "Jensen", "Jimenez", "Johns",
            "Johnston", "Jordan", "Joseph", "Joyce", "Joyner", "Juarez",
            "Justice", "Kane", "Kaufman", "Keith", "Keller", "Kelley", "Kelly",
            "Kemp", "Kennedy", "Kent", "Kerr", "Key", "Kidd", "Kim", "King",
            "Kinney", "Kirby", "Kirk", "Kirkland", "Klein", "Kline", "Knapp",
            "Knight", "Knowles", "Knox", "Koch", "Kramer", "Lamb", "Lambert",
            "Lancaster", "Landry", "Lane", "Lang", "Langley", "Lara", "Larsen",
            "Larson", "Lawrence", "Lawson", "Le", "Leach", "Leblanc", "Lee",
            "Leon", "Leonard", "Lester", "Levine", "Levy", "Lewis", "Lindsay",
            "Lindsey", "Little", "Livingston", "Lloyd", "Logan", "Long",
            "Lopez", "Lott", "Love", "Lowe", "Lowery", "Lucas", "Luna",
            "Lynch", "Lynn", "Lyons", "Macdonald", "Macias", "Mack", "Madden",
            "Maddox", "Maldonado", "Malone", "Mann", "Manning", "Marks",
            "Marquez", "Marsh", "Marshall", "Martin", "Martinez", "Mason",
            "Massey", "Mathews", "Mathis", "Matthews", "Maxwell", "May",
            "Mayer", "Maynard", "Mayo", "Mays", "McBride", "McCall",
            "McCarthy", "McCarty", "McClain", "McClure", "McConnell",
            "McCormick", "McCoy", "McCray", "McCullough", "McDaniel",
            "McDonald", "McDowell", "McFadden", "McFarland", "McGee",
            "McGowan", "McGuire", "McIntosh", "McIntyre", "McKay", "McKee",
            "McKenzie", "McKinney", "McKnight", "McLaughlin", "McLean",
            "McLeod", "McMahon", "McMillan", "McNeil", "McPherson", "Meadows",
            "Medina", "Mejia", "Melendez", "Melton", "Mendez", "Mendoza",
            "Mercado", "Mercer", "Merrill", "Merritt", "Meyer", "Meyers",
            "Michael", "Middleton", "Miles", "Mills", "Miranda", "Mitchell",
            "Molina", "Monroe", "Montgomery", "Montoya", "Moody", "Moon",
            "Mooney", "Morales", "Moran", "Moreno", "Morgan", "Morin",
            "Morris", "Morrison", "Morrow", "Morse", "Morton", "Moses",
            "Mosley", "Moss", "Mueller", "Mullen", "Mullins", "Munoz",
            "Murphy", "Murray", "Myers", "Nash", "Navarro", "Neal", "Nelson",
            "Newman", "Newton", "Nguyen", "Nichols", "Nicholson", "Nielsen",
            "Nieves", "Nixon", "Noble", "Noel", "Nolan", "Norman", "Norris",
            "Norton", "Nunez", "Obrien", "Ochoa", "Oconnor", "Odom",
            "Odonnell", "Oliver", "Olsen", "Olson", "O'neal", "O'neil",
            "O'neill", "Orr", "Ortega", "Ortiz", "Osborn", "Osborne", "Owen",
            "Owens", "Pace", "Pacheco", "Padilla", "Page", "Palmer", "Park",
            "Parker", "Parks", "Parrish", "Parsons", "Pate", "Patel",
            "Patrick", "Patterson", "Patton", "Paul", "Payne", "Pearson",
            "Peck", "Pena", "Pennington", "Perez", "Perkins", "Perry",
            "Peters", "Petersen", "Peterson", "Petty", "Phelps", "Phillips",
            "Pickett", "Pierce", "Pittman", "Pitts", "Pollard", "Poole",
            "Pope", "Porter", "Potter", "Potts", "Powell", "Powers", "Pratt",
            "Preston", "Price", "Prince", "Pruitt", "Puckett", "Pugh", "Quinn",
            "Ramirez", "Ramos", "Ramsey", "Randall", "Randolph", "Rasmussen",
            "Ratliff", "Ray", "Raymond", "Reed", "Reese", "Reeves", "Reid",
            "Reilly", "Reyes", "Reynolds", "Rhodes", "Rice", "Rich", "Richard",
            "Richards", "Richardson", "Richmond", "Riddle", "Riggs", "Riley",
            "Rios", "Rivas", "Rivera", "Rivers", "Roach", "Robbins",
            "Roberson", "Roberts", "Robertson", "Robinson", "Robles", "Rocha",
            "Rodgers", "Rodriguez", "Rodriquez", "Rogers", "Rojas", "Rollins",
            "Roman", "Romero", "Rosa", "Rosales", "Rosario", "Rose", "Ross",
            "Roth", "Rowe", "Rowland", "Roy", "Ruiz", "Rush", "Russell",
            "Russo", "Rutledge", "Ryan", "Salas", "Salazar", "Salinas",
            "Sampson", "Sanchez", "Sanders", "Sandoval", "Sanford", "Santana",
            "Santiago", "Santos", "Sargent", "Saunders", "Savage", "Sawyer",
            "Schmidt", "Schneider", "Schroeder", "Schultz", "Schwartz",
            "Scott", "Sears", "Sellers", "Serrano", "Sexton", "Shaffer",
            "Shannon", "Sharp", "Sharpe", "Shaw", "Shelton", "Shepard",
            "Shepherd", "Sheppard", "Sherman", "Shields", "Short", "Silva",
            "Simmons", "Simon", "Simpson", "Sims", "Singleton", "Skinner",
            "Slater", "Sloan", "Small", "Snider", "Snow", "Snyder", "Solis",
            "Solomon", "Sosa", "Soto", "Sparks", "Spears", "Spence", "Spencer",
            "Stafford", "Stanley", "Stanton", "Stark", "Steele", "Stein",
            "Stephens", "Stephenson", "Stevens", "Stevenson", "Stewart",
            "Stokes", "Stone", "Stout", "Strickland", "Strong", "Stuart",
            "Suarez", "Sullivan", "Summers", "Sutton", "Swanson", "Sweeney",
            "Sweet", "Sykes", "Talley", "Tanner", "Tate", "Terrell", "Terry",
            "Thompson", "Thornton", "Tillman", "Todd", "Torres", "Townsend",
            "Tran", "Travis", "Trevino", "Trujillo", "Tucker", "Turner",
            "Tyler", "Tyson", "Underwood", "Valdez", "Valencia", "Valentine",
            "Valenzuela", "Vance", "Vang", "Vargas", "Vasquez", "Vaughan",
            "Vaughn", "Vazquez", "Vega", "Velasquez", "Velazquez", "Velez",
            "Van halen", "Vincent", "Vinson", "Wade", "Wagner", "Walker",
            "Wall", "Wallace", "Waller", "Walls", "Walsh", "Walter", "Walters",
            "Walton", "Ward", "Ware", "Warner", "Warren", "Washington",
            "Waters", "Watkins", "Watson", "Watts", "Weaver", "Webb", "Weber",
            "Webster", "Weeks", "Weiss", "Welch", "Wells", "West", "Wheeler",
            "Whitaker", "White", "Whitehead", "Whitfield", "Whitley",
            "Whitney", "Wiggins", "Wilcox", "Wilder", "Wiley", "Wilkerson",
            "Wilkins", "Wilkinson", "William", "Williamson", "Willis",
            "Winters", "Wise", "Witt", "Wolf", "Wolfe", "Wong", "Wood",
            "Woodard", "Woods", "Woodward", "Wooten", "Workman", "Wright",
            "Wyatt", "Wynn", "Yang", "Yates", "York", "Young", "Zamora",
            "Zimmerman"
    };
}