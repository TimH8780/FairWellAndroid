package io.github.budgetninja.fairwellandroid;

import android.util.Pair;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.Date;
import java.util.List;
import io.github.budgetninja.fairwellandroid.FriendObject.Friend;

/**
 *Created by Tim on 11/07/15.
 */
public class StatementObject {

    public static class SummaryStatement {

        String description, category;
        Date date, deadline;
        int mode, unknown;
        double totalAmount, unknownAmount;
        String submitBy;
        Friend payee;
        List<Pair<Friend,Double>> payer;

        public SummaryStatement(String description, String category, Date date, Date deadline, int mode, int unknown, double unknownAmount,
                                double totalAmount, String submitBy, Friend payee, List<Pair<Friend, Double>> payer){
            this.description = description;
            this.category = category;
            this.date = date;
            this.deadline = deadline;
            this.mode = mode;
            this.unknown = unknown;
            this.unknownAmount = unknownAmount;
            this.totalAmount = totalAmount;
            this.submitBy = submitBy;
            this.payee = payee;
            this.payer = payer;
        }

    }

    public static class Statement implements Comparable<Statement>{

        String description, category;
        Date date, deadline;
        int mode, unknown;
        double totalAmount, unknownAmount;
        String submitBy;
        ParseUser payee;
        List<ParseObject> payer;

        public Statement(String description, String category, Date date, Date deadline, int mode, int unknown, double unknownAmount,
                                double totalAmount, String submitBy, ParseUser payee, List<ParseObject> payer){
            this.description = description;
            this.category = category;
            this.date = date;
            this.deadline = deadline;
            this.mode = mode;
            this.unknown = unknown;
            this.unknownAmount = unknownAmount;
            this.totalAmount = totalAmount;
            this.submitBy = submitBy;
            this.payee = payee;
            this.payer = payer;
        }

        @Override
        public int compareTo(Statement another) {
            return deadline.compareTo(another.deadline);
        }
    }
}
