package lectureinitializer;

import java.io.*;
import java.nio.file.*;
import java.time.format.*;
import java.util.*;

public class ProtocolFileWriter {

    public static void writeProtocolFile(
        final Path protocols,
        final TalkMode talkMode,
        final Subject subject,
        final String place,
        final TalkAssignment assignment
    ) throws IOException {
        final String[] nameParts = assignment.topicAssignment().participant().split(" ");
        final String lastName = nameParts[nameParts.length - 1];
        final String protocol = String.format("protokoll%s%s.tex", subject.shortName(), ProtocolFileWriter.toASCII(lastName));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(protocols.resolve(protocol).toFile()))) {
            writer.write("\\documentclass{article}\n\n");
            writer.write("\\input{../../../../../../templates/protocol/packages.tex}\n");
            writer.write("\\newcommand{\\subject}{");
            writer.write(subject.name());
            writer.write("}\n");
            writer.write("\\newcommand{\\student}{");
            writer.write(assignment.topicAssignment().participant());
            writer.write("}\n");
            writer.write("\\newcommand{\\presentationtitle}{");
            writer.write(assignment.topicAssignment().topic());
            writer.write("}\n");
            writer.write("\\newcommand{\\presentationdate}{");
            writer.write(String.valueOf(assignment.date().getDayOfMonth()));
            writer.write(".\\ ");
            writer.write(assignment.date().getMonth().getDisplayName(TextStyle.FULL, Locale.GERMAN));
            writer.write(" ");
            writer.write(String.valueOf(assignment.date().getYear()));
            writer.write("}\n");
            writer.write("\\newcommand{\\presentationplace}{");
            writer.write(place);
            writer.write("}\n");
            if (talkMode == TalkMode.TALK80QUIZ20) {
                writer.write("\\setboolean{mandatoryhandout}{false}\n");
            }
            writer.write("\n");
            writer.write("\\newcommand{\\presentationContent}{%\n");
            writer.write("Der Vortrag behandelte das Thema \\presentationtitle.\\\\[2ex]\n");
            writer.write("\\notes{%\n");
            writer.write("\\item Start: \n");
            writer.write("\\item \n");
            writer.write("\\item Ende Vortrag: \n");
            writer.write("\\item Prüfer: ?\n");
            writer.write("\\item Ende Diskussion: \n");
            writer.write("}\n}\n\n");
            switch (talkMode) {
            case TALK80QUIZ20:
                writer.write("\\newcommand{\\presentationUnderstandability}{%\n");
                writer.write("\\understandingstructureviii{}\n");
                writer.write("\\understandinglogicviii{}\n");
                writer.write("\\understandingspeechviii{}\n");
                writer.write("\\understandingexamplesviii{}\n");
                writer.write("\\understandingvisualizationviii{}\n");
                writer.write("\\evaluationpartresult{40}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationDepth}{%\n");
                writer.write("\\contenttimeiv{}\n");
                writer.write("\\contentdepthiv{}\n");
                writer.write("\\contentbreadthiv{}\n");
                writer.write("\\contentcorrectnessiv{}\n");
                writer.write("\\contentquestionsiv{}\n");
                writer.write("\\evaluationpartresult{20}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationApplication}{%\n");
                writer.write("\\applicationrelevanceviii{}\n");
                writer.write("\\applicationdemonstrationvi{}\n");
                writer.write("\\applicationusersvi{}\n");
                writer.write("\\evaluationpartresult{20}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\quiz}{%\n");
                writer.write("\\quizcontentv{}\n");
                writer.write("\\quizdifficultyv{}\n");
                ProtocolFileWriter.writeQuizCommands(writer);
                writer.write("\\evaluationpartresult{20}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\additionalEvaluation}{}\n\n");
                break;
            case TALK40QUIZ10:
            case TALK40QUIZ20:
                writer.write("\\newcommand{\\presentationUnderstandability}{%\n");
                writer.write("\\understandingstructureiv{}\n");
                writer.write("\\understandinglogiciv{}\n");
                writer.write("\\understandingspeechiv{}\n");
                writer.write("\\understandingexamplesiv{}\n");
                writer.write("\\understandingvisualizationiv{}\n");
                writer.write("\\evaluationpartresult{20}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationDepth}{%\n");
                writer.write("\\contenttimeii{}\n");
                writer.write("\\contentdepthii{}\n");
                writer.write("\\contentbreadthii{}\n");
                writer.write("\\contentcorrectnessii{}\n");
                writer.write("\\contentquestionsii{}\n");
                writer.write("\\evaluationpartresult{10}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationApplication}{%\n");
                writer.write("\\applicationrelevanceiv{}\n");
                writer.write("\\applicationdemonstrationiii{}\n");
                writer.write("\\applicationusersiii{}\n");
                writer.write("\\evaluationpartresult{10}\n");
                writer.write("}\n\n");
                if (talkMode == TalkMode.TALK40QUIZ10) {
                    writer.write("\\newcommand{\\handout}{%\n");
                    writer.write("\\handoutdefault{}\n");
                    writer.write("\\handoutamountiii{}\n");
                    writer.write("\\handoutqualityiv{}\n");
                    writer.write("\\handoutformaliii{}\n");
                    writer.write("\\evaluationpartresult{10}\n");
                    writer.write("}\n\n");
                    writer.write("\\newcommand{\\quiz}{%\n");
                    ProtocolFileWriter.writeQuizCommands(writer);
                    writer.write("\\evaluationpartresult{10}\n");
                } else {
                    writer.write("\\newcommand{\\quiz}{%\n");
                    writer.write("\\quizcontentv{}\n");
                    writer.write("\\quizdifficultyv{}\n");
                    ProtocolFileWriter.writeQuizCommands(writer);
                    writer.write("\\evaluationpartresult{20}\n");
                }
                writer.write("}\n\n");
                writer.write("\\newcommand{\\additionalEvaluation}{%\n");
                writer.write("\\contributions\n");
                writer.write("Die individuellen Beiträge umfassten:\n");
                writer.write("\\begin{itemize}\n");
                writer.write("\\item \\contributionvalue{0}\n");
                writer.write("\\end{itemize}%\n");
                writer.write("\\evaluationpartresult{40}\n");
                writer.write("}\n\n");
                break;
            case TALK50SCIENCE:
                writer.write("\\newcommand{\\presentationUnderstandability}{%\n");
                writer.write("\\understandingstructurev{}\n");
                writer.write("\\understandinglogicv{}\n");
                writer.write("\\understandingspeechv{}\n");
                writer.write("\\understandingexamplesv{}\n");
                writer.write("\\understandingvisualizationv{}\n");
                writer.write("\\evaluationpartresult{25}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationDepth}{%\n");
                writer.write("\\contenttimeiii{}\n");
                writer.write("\\contentdepthiii{}\n");
                writer.write("\\contentbreadthiii{}\n");
                writer.write("\\contentcorrectnessiii{}\n");
                writer.write("\\contentquestionsiii{}\n");
                writer.write("\\evaluationpartresult{15}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationApplication}{%\n");
                writer.write("\\applicationrelevanceiv{}\n");
                writer.write("\\applicationdemonstrationiii{}\n");
                writer.write("\\applicationusersiii{}\n");
                writer.write("\\evaluationpartresult{10}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\paperEvaluation}{%\n");
                writer.write("\\goali{}\n");
                writer.write("\\contributionsi{}\n");
                writer.write("\\structurequalityi{}\n");
                writer.write("\\basicsmatchingi{}\n");
                writer.write("\\conclusioni{}\n\n");
                writer.write("\\literatureamounti{}\n");
                writer.write("\\literaturequalityii{}\n");
                writer.write("\\relatedamounti{}\n");
                writer.write("\\relatedqualityii{}\n");
                writer.write("\\quotingdensityii{}\n");
                writer.write("\\methodapplicationii{}\n");
                writer.write("\\methodintroi{}\n");
                writer.write("\\objectivityi{}\n");
                writer.write("\\reliabilityi{}\n");
                writer.write("\\validityi{}\n");
                writer.write("\\comprehensibilityi{}\n\n");
                writer.write("Folgende inhaltliche Beiträge wurden für die Arbeit ausgewählt:\n");
                writer.write("\\begin{itemize}\n");
                writer.write("\\item Beitrag 1\n");
                writer.write("\\end{itemize}\n");
                writer.write("\\innovativenessii{}\n");
                writer.write("\\relevanceii{}\n");
                writer.write("\\levelii{}\n");
                writer.write("\\applicabilityii{}\n");
                writer.write("\\valueii{}\n\n");
                writer.write("\\appearancei{}\n");
                writer.write("\\spellingautoi{}\n");
                writer.write("\\languagei{}\n");
                writer.write("\\figuresi{}\n");
                writer.write("\\literaturestylei{}\n\n");
                writer.write("\\evaluationpartresult{35}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\reviewi}{% max 5\n");
                writer.write("Das erste Gutachten wurde zur Ausarbeitung mit dem Titel \n");
                writer.write("\\glqq{}X\\grqq{} \n");
                writer.write("verfasst.\n");
                writer.write("\\reviewstylei{}\n");
                writer.write("\\reviewsummaryi{}\n");
                writer.write("\\reviewcriteriai{}\n");
                writer.write("\\reviewsuggestionsi{}\n");
                writer.write("\\reviewevaluationi{}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\reviewii}{% max 5\n");
                writer.write("Das zweite Gutachten wurde zur Ausarbeitung mit dem Titel \n");
                writer.write("\\glqq{}X\\grqq{} \n");
                writer.write("verfasst.\n");
                writer.write("\\reviewstylei{}\n");
                writer.write("\\reviewsummaryi{}\n");
                writer.write("\\reviewcriteriai{}\n");
                writer.write("\\reviewsuggestionsi{}\n");
                writer.write("\\reviewevaluationi{}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\reviewiii}{% max 5\n");
                writer.write("Das dritte Gutachten wurde zur Ausarbeitung mit dem Titel \n");
                writer.write("\\glqq{}X\\grqq{} \n");
                writer.write("verfasst.\n");
                writer.write("\\reviewstylei{}\n");
                writer.write("\\reviewsummaryi{}\n");
                writer.write("\\reviewcriteriai{}\n");
                writer.write("\\reviewsuggestionsi{}\n");
                writer.write("\\reviewevaluationi{}\n");
                writer.write("}\n\n");
                break;
            }
            writer.write("\\newcommand{\\totalReview}{%\n");
            writer.write(
                "Insgesamt wurden \\evaluationpoints{} Punkte erreicht und das Gesamturteil lautet: \\grade\n"
            );
            writer.write("}\n\n");
            if (talkMode == TalkMode.TALK50SCIENCE) {
                writer.write("\\input{../../../../../../templates/protocol/protocolScience.tex}\n");
            } else {
                writer.write("\\input{../../../../../../templates/protocol/protocol.tex}\n");
            }
        }
    }

    private static String toASCII(final String name) {
        return name
            .replaceAll("ä", "ae")
            .replaceAll("Ä", "Ae")
            .replaceAll("ö", "oe")
            .replaceAll("Ö", "Oe")
            .replaceAll("ü", "ue")
            .replaceAll("Ü", "Ue")
            .replaceAll("ß", "ss")
            .replaceAll("é", "e")
            .replaceAll("[^\\x00-\\x7F]", "");
    }

    private static void writeQuizCommands(final BufferedWriter writer) throws IOException {
        writer.write("\\quizpassed{}\n");
        writer.write("\\quizbonusi{}\n");
        writer.write("\\quizbonusii{}\n");
        writer.write("\\quizbonusiii{}\n");
        writer.write("\\quizparticipantbonusi{}\n");
        writer.write("\\quizparticipantbonusii{}\n");
        writer.write("\\quizparticipantbonusiii{}\n");
    }

}
