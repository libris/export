/**
 * A wrapper for the various tasks, allowing them to be run from a prebuilt jar
 * without gradle, for performance reasons.
 */
public class Main {

    public static void main(String[] args){

        if (args.length < 1) {
            println("Please pass a task as a parameter (for example \"ListChanges_xl\") at the end of your command.")
            return
        }

        String[] filteredArgs = new String[args.length-1]
        for (int i = 0; i < args.length-1; ++i)
            filteredArgs[i] = args[i+1]

        switch (args[0]) {
            case "GetRecords":
                GetRecords.main(filteredArgs)
                break
            case "ListChanges":
                if (filteredArgs[0].startsWith("-Prange="))
                    filteredArgs = filteredArgs[0].substring("-Prange=".length()).split(",")
                ListChanges.main(filteredArgs)
                break
            case "GetRecords_xl":
                GetRecords_xl.main(filteredArgs)
                break
            case "ListChanges_xl":
                if (filteredArgs[0].startsWith("-Prange="))
                    filteredArgs = filteredArgs[0].substring("-Prange=".length()).split(",")
                ListChanges_xl.main(filteredArgs)
                break
        }
    }

}