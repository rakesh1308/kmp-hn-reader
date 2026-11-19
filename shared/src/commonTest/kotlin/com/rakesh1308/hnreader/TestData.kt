package com.rakesh1308.hnreader

import com.rakesh1308.hnreader.domain.model.Story

fun story(
    id: Long,
    title: String = "Story $id",
    url: String? = "https://www.example.com/post/$id",
    author: String = "alice",
    score: Int = 10,
    comments: Int = 2,
    postedAt: Long = 1_000,
) = Story(id, title, url, author, score, comments, postedAt)
