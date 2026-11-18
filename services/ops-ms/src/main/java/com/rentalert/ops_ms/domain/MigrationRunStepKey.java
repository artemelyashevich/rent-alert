package com.rentalert.ops_ms.domain;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MigrationRunStepKey implements Serializable {

    private UUID migrationId;

    private int stepOrder;
}
