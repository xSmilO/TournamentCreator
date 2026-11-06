package com.hltv.tournamentcreator.dao;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
interface ConnectionSource {
    Connection open() throws SQLException;
}
