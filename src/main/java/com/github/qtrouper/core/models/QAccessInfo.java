package com.github.qtrouper.core.models;

import lombok.*;

/**
 * @author koushik
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QAccessInfo {

    private boolean idempotencyCheckRequired;

}
