package classes;

public class Book
{
    public String name;
    public String author;
    public int price;
    public String img;
    public String ref;
    public String ISBN;

    public Book(){}
    public Book(String inName) {name = inName;}

    @Override
    public String toString()
    {
        return "Book " + name + " author: " + author + " price = " + price +
                " img=" + img + " ref=" + ref;
    }
}
