import org.json.JSONException;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.checks.DeadStoreCheck;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DeadStoreProcessor extends AbstractProcessor<CtStatement> {

    private Set<Bug> SetOfBugs;//set of bugs, corresponding to jsonArray
    private Set<Long> SetOfLineNumbers;//set of line numbers corresponding to bugs, just for efficiency
    private Set<String> SetOfFileNames;//-----
    private Bug thisBug;               //current bug. This is set inside isToBeProcessed function
    private String thisBugName;        //name (message) of current thisBug.
    String var;//contains name of variable which is uselessly assigned in the current bug.
    public DeadStoreProcessor(List<File> files)  {
        Set<AnalyzerMessage> total = new HashSet<>();
        for(File file :files)
        {
            Set<AnalyzerMessage> verify = JavaCheckVerifier.verify(file.getAbsolutePath(), new DeadStoreCheck(), true);
            total.addAll(verify);
        }
        SetOfBugs = Bug.createSetOfBugs(total);
        SetOfLineNumbers=new HashSet<>();
        SetOfFileNames=new HashSet<>();
        thisBug=new Bug();
        for(Bug bug:SetOfBugs)
        {
            SetOfLineNumbers.add(bug.getLineNumber());
            SetOfFileNames.add(bug.getFileName());
        }
    }


    @Override
    public boolean isToBeProcessed(CtStatement element)
    {
        if(element==null)
        {
            return false;
        }
        long line =-1;
        String targetName="",fileOfElement="";
        if(element instanceof CtLocalVariable)
        {
            targetName = ((CtLocalVariable)element).getSimpleName();
            line=(long) element.getPosition().getLine();
            fileOfElement=element.getPosition().getFile().getName();
        }
        else if(element instanceof CtAssignment)
        {
            targetName=((CtAssignment) element).getAssigned().toString();
            line=(long) element.getPosition().getLine();
            fileOfElement=element.getPosition().getFile().getName();
        }
        else return false;

        if(!SetOfLineNumbers.contains(line)||!SetOfFileNames.contains(fileOfElement))
        {
            return false;
        }
        try {
            thisBug = new Bug();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(Bug bug:SetOfBugs)
        {
            if(bug.getLineNumber()!=line||!bug.getFileName().equals(fileOfElement))
            {
                continue;
            }

            String bugName=bug.getName();
            String[] split = bugName.split("\"");
            for(String bugword:split)
            {
                if(targetName.equals(bugword))
                {
                    try
                    {
                        thisBug = new Bug(bug);
                        thisBugName = bugword;
                        var=targetName;
                        return true;
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }
    @Override
    public void process(CtStatement element) {
        System.out.println("BUG\n");
        CtCodeSnippetStatement snippet = getFactory().Core().createCodeSnippetStatement();
        final String value = String.format("//[Spoon inserted check], repairs sonarqube rule 1854:Dead stores should be removed,\n//useless assignment to %s removed",
                var);
        snippet.setValue(value);
        element.delete();
    }
}