/**
 * Strategic Leave Planner — Pure Optimization Engine
 *
 * Framework-agnostic, zero-DOM implementation ready to drop into any JS/TS app.
 * Uses local YYYY-MM-DD date strings throughout to avoid UTC timezone shifts.
 */

// ---------------------------------------------------------------------------
// Date Helpers (local-time safe)
// ---------------------------------------------------------------------------

export function formatLocalDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

export function parseLocalDate(dateStr) {
    if (!dateStr) return null;
    const parts = dateStr.split('-');
    if (parts.length !== 3) return null;
    const [y, m, d] = parts.map(Number);
    return new Date(y, m - 1, d);
}

export function getAdjacentDateStr(dateStr, offset) {
    const date = parseLocalDate(dateStr);
    if (!date) return null;
    date.setDate(date.getDate() + offset);
    return formatLocalDate(date);
}

export function defaultIsSunday(date) {
    return date.getDay() === 0;
}

export function defaultIsSecondSaturday(date) {
    const dayOfMonth = date.getDate();
    return date.getDay() === 6 && dayOfMonth >= 8 && dayOfMonth <= 14;
}

export function getDaysInMonth(year, month) {
    const dates = [];
    const startDate = new Date(year, month, 1);
    const startDay = startDate.getDay();
    for (let i = 0; i < startDay; i++) dates.push(null);

    const numDays = new Date(year, month + 1, 0).getDate();
    for (let i = 1; i <= numDays; i++) {
        dates.push(new Date(year, month, i));
    }
    return dates;
}

// ---------------------------------------------------------------------------
// Core Planner Engine
// ---------------------------------------------------------------------------

/**
 * Generates an optimized leave plan from `startDateStr` through Dec 31 of that year.
 *
 * @param {Object} params
 * @param {string} params.startDateStr - Start date in YYYY-MM-DD format
 * @param {Object} params.balances - Unplanned leave counts { cl, rh, comp, el }
 * @param {Array}  [params.compCredits=[]] - Active comp credits: [{ id, date, remainingDays, expiryDate, isExpired }]
 * @param {Array}  [params.transactions=[]] - Existing leave transactions
 * @param {Array}  [params.customHolidays=[]] - Govt/company holiday date strings ['YYYY-MM-DD']
 * @param {Object} [params.holidayDetails={}] - Map of YYYY-MM-DD -> holiday name
 * @param {Array}  [params.restrictedHolidays=[]] - Restricted holiday date strings ['YYYY-MM-DD']
 * @param {Set}    [params.blockedDates=new Set()] - Dates excluded from planning
 * @param {Object} [params.pinnedDates={}] - Map of YYYY-MM-DD -> 'AUTO' | 'CL' | 'RH' | 'Compensatory_Used' | 'EL_Used'
 * @param {Set}    [params.plannedToWorkHolidays=new Set()] - Weekends/holidays user plans to work (+1 virtual Comp)
 * @param {Object} [params.options={}] - Strategy options
 * @returns {Array<Object>} Sorted array of continuous off-day blocks
 */
export function generateLeavePlan({
    startDateStr,
    balances,
    compCredits = [],
    transactions = [],
    customHolidays = [],
    holidayDetails = {},
    restrictedHolidays = [],
    blockedDates = new Set(),
    pinnedDates = {},
    plannedToWorkHolidays = new Set(),
    options = {},
}) {
    const {
        includeCL = true,
        includeRH = true,
        includeComp = true,
        includeEL = true,
        planningOrder = 'dates-first',       // 'dates-first' | 'type-priority'
        proximityPreference = 'bridge',      // 'bridge' | 'mid-week'
        leaveTypePriority = ['Comp', 'CL', 'EL'],
        spreadMode = 'balanced',             // 'balanced' | 'spread'
        limitCompExpiry = false,
        compExpiryThreshold = 120,
        compExpiryDays = 180,
        isWeekendFn = (d) => defaultIsSunday(d) || defaultIsSecondSaturday(d),
    } = options;

    let clLeft = includeCL ? (balances?.cl ?? 0) : 0;
    let rhLeft = includeRH ? (balances?.rh ?? 0) : 0;
    let compLeft = includeComp ? (balances?.comp ?? 0) : 0;
    let elLeft = includeEL ? (balances?.el ?? 0) : 0;

    if (clLeft <= 0 && rhLeft <= 0 && compLeft <= 0 && elLeft <= 0 && Object.keys(pinnedDates).length === 0) {
        return [];
    }

    const planStart = parseLocalDate(startDateStr);
    if (!planStart || isNaN(planStart.getTime())) return [];
    const planEnd = new Date(planStart.getFullYear(), 11, 31);

    // Subtract explicit pinned leaves from remaining quotas
    Object.values(pinnedDates).forEach((type) => {
        if (type === 'CL') clLeft = Math.max(0, clLeft - 1);
        if (type === 'RH') rhLeft = Math.max(0, rhLeft - 1);
        if (type === 'EL_Used') elLeft = Math.max(0, elLeft - 1);
        if (type === 'Compensatory_Used') compLeft = Math.max(0, compLeft - 1);
    });

    const penalty = spreadMode === 'spread' ? 3.0 : 1.5;

    // 1. Gather active Comp Off credits + virtual credits from worked holidays
    let activeCompCredits = includeComp
        ? compCredits
              .filter((c) => c.remainingDays > 0 && !c.isExpired && c.expiryDate >= startDateStr)
              .map((c) => ({ ...c }))
        : [];

    if (includeComp && limitCompExpiry) {
        activeCompCredits = activeCompCredits.filter((c) => {
            const expDate = parseLocalDate(c.expiryDate);
            const daysFromDec31 = Math.ceil((expDate - planEnd) / (1000 * 60 * 60 * 24));
            return daysFromDec31 <= compExpiryThreshold;
        });
    }

    plannedToWorkHolidays.forEach((dateStr) => {
        const earnedDate = parseLocalDate(dateStr);
        if (!earnedDate) return;
        const expiryDate = new Date(earnedDate);
        expiryDate.setDate(earnedDate.getDate() + compExpiryDays);
        const expiryDateStr = formatLocalDate(expiryDate);
        const daysFromDec31 = Math.ceil((expiryDate - planEnd) / (1000 * 60 * 60 * 24));

        activeCompCredits.push({
            id: `virtual-credit-${dateStr}`,
            date: dateStr,
            originalDays: 1,
            usedDays: 0,
            remainingDays: 1,
            expiryDate: expiryDateStr,
            daysFromDec31,
            isExpired: daysFromDec31 < -365,
            isVirtual: true,
        });
    });

    // 2. Build Calendar Matrix
    const calendar = {};
    const holidaySet = new Set(customHolidays);
    const rhSet = new Set(restrictedHolidays);

    const hasNonCreditLeave = (dateStr) =>
        transactions.some((t) => {
            if (t.type.includes('Credit') || t.type === 'Compensatory_Earned') return false;
            return t.date === dateStr;
        });

    for (let d = new Date(planStart); d <= planEnd; d.setDate(d.getDate() + 1)) {
        const dateStr = formatLocalDate(d);
        const isWorkedHoliday = plannedToWorkHolidays.has(dateStr);
        const isWeekendOrHoliday = (isWeekendFn(d) || holidaySet.has(dateStr)) && !isWorkedHoliday;
        const hasLeave = hasNonCreditLeave(dateStr);

        calendar[dateStr] = {
            dateStr,
            dayOfWeek: d.getDay(),
            isWeekendOrHoliday,
            hasExistingLeave: hasLeave,
            isInitialDayOff: isWeekendOrHoliday || hasLeave,
            plannedType: null,
            linkedCreditId: null,
            isRestrictedHoliday: rhSet.has(dateStr) && !isWorkedHoliday,
        };
    }

    // Apply explicit pinned leaves first
    Object.entries(pinnedDates).forEach(([dateStr, type]) => {
        if (calendar[dateStr] && type !== 'AUTO') {
            calendar[dateStr].plannedType = type;
            if (type === 'Compensatory_Used') {
                activeCompCredits.sort((a, b) => a.expiryDate.localeCompare(b.expiryDate));
                const eligible = activeCompCredits.find(
                    (c) => c.remainingDays > 0 && dateStr >= startDateStr && dateStr <= c.expiryDate
                );
                if (eligible) {
                    calendar[dateStr].linkedCreditId = eligible.id;
                    eligible.remainingDays--;
                }
            }
        }
    });

    const isCalendarDayOff = (dateStr, currentSelected) => {
        if (calendar[dateStr]) {
            return (
                calendar[dateStr].isInitialDayOff ||
                !!calendar[dateStr].plannedType ||
                (currentSelected && currentSelected.has(dateStr))
            );
        }
        const d = parseLocalDate(dateStr);
        if (!d || isNaN(d.getTime())) return false;
        const isWorkedHoliday = plannedToWorkHolidays.has(dateStr);
        const isWeekendOrHoliday = (isWeekendFn(d) || holidaySet.has(dateStr)) && !isWorkedHoliday;
        return isWeekendOrHoliday || hasNonCreditLeave(dateStr) || (currentSelected && currentSelected.has(dateStr));
    };

    // 3. ROI Scoring Helper
    const getPotentialROI = (datesToBook, currentSelected) => {
        const isTempDayOff = (dObj) => {
            const dStr = formatLocalDate(dObj);
            if (datesToBook.includes(dStr)) return true;
            return isCalendarDayOff(dStr, currentSelected);
        };

        const firstDate = parseLocalDate(datesToBook[0]);
        const lastDate = parseLocalDate(datesToBook[datesToBook.length - 1]);

        let countBefore = 0;
        let curr = new Date(firstDate);
        curr.setDate(firstDate.getDate() - 1);
        while (isTempDayOff(curr)) {
            countBefore++;
            curr.setDate(curr.getDate() - 1);
        }

        let countAfter = 0;
        curr = new Date(lastDate);
        curr.setDate(lastDate.getDate() + 1);
        while (isTempDayOff(curr)) {
            countAfter++;
            curr.setDate(curr.getDate() + 1);
        }

        if (proximityPreference === 'mid-week') {
            return countBefore === 0 && countAfter === 0 ? 5.0 : 0.1;
        }

        let score;
        const isSandwich = countBefore >= 1 && countAfter >= 1;
        const isAdjacent = countBefore >= 1 || countAfter >= 1;

        if (isSandwich) {
            if (datesToBook.length === 1) score = 25.0 + countBefore + countAfter;
            else if (datesToBook.length === 2) score = 5.0 + countBefore + countAfter;
            else score = countBefore + countAfter;
        } else if (isAdjacent) {
            const dayOfWeek = firstDate.getDay();
            const isMonOrFri = dayOfWeek === 1 || dayOfWeek === 5;
            score =
                datesToBook.length === 1 && isMonOrFri
                    ? 15.0 + Math.max(countBefore, countAfter)
                    : 0.1 * Math.max(countBefore, countAfter);
        } else {
            const dayOfWeek = firstDate.getDay();
            const isMidWeek = dayOfWeek === 2 || dayOfWeek === 3 || dayOfWeek === 4;
            score = isMidWeek ? -10.0 : 0.0;
        }

        const hasRH = datesToBook.some((dStr) => calendar[dStr]?.isRestrictedHoliday);
        if (hasRH && isAdjacent) score += 10.0;

        return score;
    };

    // -----------------------------------------------------------------------
    // STEP 1: SELECT OPTIMAL LEAVE SLOTS
    // -----------------------------------------------------------------------
    const selectedSlots = new Set(Object.keys(pinnedDates));

    const getPlannedInMonth = (month) => {
        let count = 0;
        selectedSlots.forEach((dateStr) => {
            const m = Number(dateStr.split('-')[1]) - 1;
            if (m === month) count++;
        });
        return count;
    };

    const findFallbackSlot = (currentSelected) => {
        const extensions = Object.values(calendar)
            .filter(
                (c) =>
                    !c.isInitialDayOff &&
                    !currentSelected.has(c.dateStr) &&
                    !blockedDates.has(c.dateStr) &&
                    !plannedToWorkHolidays.has(c.dateStr)
            )
            .filter((c) => {
                const prevStr = getAdjacentDateStr(c.dateStr, -1);
                const nextStr = getAdjacentDateStr(c.dateStr, 1);
                return isCalendarDayOff(prevStr, currentSelected) || isCalendarDayOff(nextStr, currentSelected);
            })
            .map((c) => ({
                dateStr: c.dateStr,
                plannedInMonth: getPlannedInMonth(parseLocalDate(c.dateStr).getMonth()),
            }));

        if (extensions.length > 0) {
            extensions.sort((a, b) => a.plannedInMonth - b.plannedInMonth || a.dateStr.localeCompare(b.dateStr));
            return extensions[0].dateStr;
        }

        const workDays = Object.values(calendar)
            .filter(
                (c) =>
                    !c.isInitialDayOff &&
                    !currentSelected.has(c.dateStr) &&
                    !blockedDates.has(c.dateStr) &&
                    !plannedToWorkHolidays.has(c.dateStr)
            )
            .map((c) => {
                const d = parseLocalDate(c.dateStr);
                const dow = d.getDay();
                return {
                    dateStr: c.dateStr,
                    plannedInMonth: getPlannedInMonth(d.getMonth()),
                    isMidWeek: dow === 2 || dow === 3 || dow === 4,
                };
            });

        if (workDays.length > 0) {
            workDays.sort((a, b) => {
                if (a.isMidWeek !== b.isMidWeek) return a.isMidWeek ? 1 : -1;
                return a.plannedInMonth - b.plannedInMonth || a.dateStr.localeCompare(b.dateStr);
            });
            return workDays[0].dateStr;
        }
        return null;
    };

    const typeSpecificPinnedCount = Object.values(pinnedDates).filter((t) => t !== 'AUTO').length;
    const totalSlotsNeeded = clLeft + (includeRH ? rhLeft : 0) + (includeComp ? compLeft : 0) + elLeft + typeSpecificPinnedCount;

    const addRhCandidates = (currentSelected) => {
        if (rhLeft <= 0) return;
        const candidateRhs = Object.values(calendar)
            .filter(
                (c) =>
                    c.isRestrictedHoliday &&
                    !c.isInitialDayOff &&
                    !blockedDates.has(c.dateStr) &&
                    !plannedToWorkHolidays.has(c.dateStr) &&
                    (!currentSelected.has(c.dateStr) || pinnedDates[c.dateStr] === 'AUTO')
            )
            .map((c) => {
                const m = parseLocalDate(c.dateStr).getMonth();
                const adjustedRoi = getPotentialROI([c.dateStr], currentSelected) - getPlannedInMonth(m) * penalty;
                return { dateStr: c.dateStr, adjustedRoi };
            })
            .sort((a, b) => b.adjustedRoi - a.adjustedRoi || a.dateStr.localeCompare(b.dateStr));

        let added = 0;
        for (const c of candidateRhs) {
            if (added >= rhLeft) break;
            const isNew = !currentSelected.has(c.dateStr);
            if (!isNew || currentSelected.size < totalSlotsNeeded) {
                currentSelected.add(c.dateStr);
                added++;
            }
        }
    };

    const compSlotsByCredit = {};
    const addCompCandidates = (currentSelected) => {
        activeCompCredits.sort((a, b) => a.expiryDate.localeCompare(b.expiryDate));
        let remainingQuota = compLeft;

        activeCompCredits.forEach((credit) => {
            if (remainingQuota <= 0 || credit.remainingDays <= 0) return;
            const daysToAllocate = Math.min(credit.remainingDays, remainingQuota);
            if (!compSlotsByCredit[credit.id]) compSlotsByCredit[credit.id] = [];

            for (let i = 0; i < daysToAllocate; i++) {
                const candidates = Object.values(calendar)
                    .filter(
                        (c) =>
                            c.dateStr >= startDateStr &&
                            c.dateStr <= credit.expiryDate &&
                            !c.isInitialDayOff &&
                            (!currentSelected.has(c.dateStr) || pinnedDates[c.dateStr] === 'AUTO') &&
                            !blockedDates.has(c.dateStr) &&
                            !plannedToWorkHolidays.has(c.dateStr)
                    )
                    .map((c) => {
                        const m = parseLocalDate(c.dateStr).getMonth();
                        const adjustedRoi = getPotentialROI([c.dateStr], currentSelected) - getPlannedInMonth(m) * (penalty * 0.6);
                        return { dateStr: c.dateStr, adjustedRoi };
                    })
                    .sort((a, b) => b.adjustedRoi - a.adjustedRoi || a.dateStr.localeCompare(b.dateStr));

                const chosen = candidates.find((c) => currentSelected.has(c.dateStr) || currentSelected.size < totalSlotsNeeded);
                if (!chosen) break;

                currentSelected.add(chosen.dateStr);
                compSlotsByCredit[credit.id].push(chosen.dateStr);
                remainingQuota--;
            }
        });
    };

    const addTypeQuotaCandidates = (currentSelected, quotaCount) => {
        let needed = Math.min(quotaCount, totalSlotsNeeded - currentSelected.size);
        while (needed > 0) {
            let best = null;
            Object.values(calendar)
                .filter(
                    (c) =>
                        !c.isInitialDayOff &&
                        !currentSelected.has(c.dateStr) &&
                        !blockedDates.has(c.dateStr) &&
                        !plannedToWorkHolidays.has(c.dateStr)
                )
                .forEach((c) => {
                    const m = parseLocalDate(c.dateStr).getMonth();
                    const rawRoi = getPotentialROI([c.dateStr], currentSelected);
                    const adjustedRoi = rawRoi - getPlannedInMonth(m) * penalty;
                    if (!best || adjustedRoi > best.adjustedRoi) {
                        best = { dateStr: c.dateStr, rawRoi, adjustedRoi };
                    }
                });

            if (best && best.rawRoi > 0) {
                currentSelected.add(best.dateStr);
            } else {
                const fallback = findFallbackSlot(currentSelected);
                if (fallback) currentSelected.add(fallback);
                else break;
            }
            needed--;
        }
    };

    if (planningOrder === 'type-priority') {
        if (includeRH) addRhCandidates(selectedSlots);
        leaveTypePriority.forEach((type) => {
            if (type === 'Comp' && includeComp) addCompCandidates(selectedSlots);
            else if (type === 'CL' && includeCL) addTypeQuotaCandidates(selectedSlots, clLeft);
            else if (type === 'EL' && includeEL) addTypeQuotaCandidates(selectedSlots, elLeft);
        });
    } else {
        if (includeRH) addRhCandidates(selectedSlots);
        if (includeComp) addCompCandidates(selectedSlots);

        while (selectedSlots.size < totalSlotsNeeded) {
            let best = null;
            Object.values(calendar)
                .filter(
                    (c) =>
                        !c.isInitialDayOff &&
                        !selectedSlots.has(c.dateStr) &&
                        !blockedDates.has(c.dateStr) &&
                        !plannedToWorkHolidays.has(c.dateStr)
                )
                .forEach((c) => {
                    const m = parseLocalDate(c.dateStr).getMonth();
                    const plannedInMonth = getPlannedInMonth(m);

                    const rawRoi1 = getPotentialROI([c.dateStr], selectedSlots);
                    const adjustedRoi1 = rawRoi1 - plannedInMonth * penalty;
                    if (!best || adjustedRoi1 > best.adjustedRoi) {
                        best = { dates: [c.dateStr], rawRoi: rawRoi1, adjustedRoi: adjustedRoi1 };
                    }

                    if (totalSlotsNeeded - selectedSlots.size >= 2) {
                        const nextStr = getAdjacentDateStr(c.dateStr, 1);
                        if (
                            calendar[nextStr] &&
                            !calendar[nextStr].isInitialDayOff &&
                            !selectedSlots.has(nextStr) &&
                            !blockedDates.has(nextStr) &&
                            !plannedToWorkHolidays.has(nextStr)
                        ) {
                            const rawRoi2 = getPotentialROI([c.dateStr, nextStr], selectedSlots);
                            const adjustedRoi2 = rawRoi2 - plannedInMonth * penalty;
                            if (rawRoi2 > rawRoi1 && adjustedRoi2 > best.adjustedRoi) {
                                best = { dates: [c.dateStr, nextStr], rawRoi: rawRoi2, adjustedRoi: adjustedRoi2 };
                            }
                        }
                    }
                });

            if (best && best.rawRoi > 0) {
                best.dates.forEach((dStr) => selectedSlots.add(dStr));
            } else {
                const fallback = findFallbackSlot(selectedSlots);
                if (fallback) selectedSlots.add(fallback);
                else break;
            }
        }
    }

    // -----------------------------------------------------------------------
    // STEP 2: ALLOCATE LEAVE TYPES TO SELECTED SLOTS
    // -----------------------------------------------------------------------
    const slotsArray = Array.from(selectedSlots);

    if (includeRH && rhLeft > 0) {
        slotsArray
            .filter((dStr) => calendar[dStr]?.isRestrictedHoliday && !calendar[dStr].plannedType)
            .map((dStr) => ({ dateStr: dStr, roi: getPotentialROI([dStr], selectedSlots) }))
            .sort((a, b) => b.roi - a.roi || a.dateStr.localeCompare(b.dateStr))
            .slice(0, rhLeft)
            .forEach((c) => {
                calendar[c.dateStr].plannedType = 'RH';
            });
    }

    if (includeComp) {
        Object.entries(compSlotsByCredit).forEach(([creditId, dates]) => {
            dates.forEach((dStr) => {
                if (calendar[dStr] && !calendar[dStr].plannedType) {
                    calendar[dStr].plannedType = 'Compensatory_Used';
                    calendar[dStr].linkedCreditId = creditId;
                }
            });
        });
    }

    leaveTypePriority.forEach((type) => {
        if (type === 'CL' && includeCL && clLeft > 0) {
            slotsArray
                .filter((dStr) => calendar[dStr] && !calendar[dStr].plannedType)
                .map((dStr) => ({ dateStr: dStr, roi: getPotentialROI([dStr], selectedSlots) }))
                .sort((a, b) => b.roi - a.roi || a.dateStr.localeCompare(b.dateStr))
                .slice(0, clLeft)
                .forEach((c) => {
                    calendar[c.dateStr].plannedType = 'CL';
                });
        } else if (type === 'EL' && includeEL && elLeft > 0) {
            slotsArray
                .filter((dStr) => calendar[dStr] && !calendar[dStr].plannedType)
                .map((dStr) => ({ dateStr: dStr, roi: getPotentialROI([dStr], selectedSlots) }))
                .sort((a, b) => b.roi - a.roi || a.dateStr.localeCompare(b.dateStr))
                .slice(0, elLeft)
                .forEach((c) => {
                    calendar[c.dateStr].plannedType = 'EL_Used';
                });
        }
    });

    // Cleanup any remaining untyped slots
    slotsArray.forEach((dStr) => {
        if (calendar[dStr] && !calendar[dStr].plannedType) {
            const clAssigned = Object.values(calendar).filter((c) => c.plannedType === 'CL').length;
            const elAssigned = Object.values(calendar).filter((c) => c.plannedType === 'EL_Used').length;
            const rhAssigned = Object.values(calendar).filter((c) => c.plannedType === 'RH').length;

            if (includeCL && clAssigned < clLeft) calendar[dStr].plannedType = 'CL';
            else if (includeEL && elAssigned < elLeft) calendar[dStr].plannedType = 'EL_Used';
            else if (includeRH && rhAssigned < rhLeft && calendar[dStr].isRestrictedHoliday) calendar[dStr].plannedType = 'RH';
            else calendar[dStr].plannedType = 'CL';
        }
    });

    // -----------------------------------------------------------------------
    // STEP 3: GROUP INTO CONTINUOUS OFF-DAY BLOCKS
    // -----------------------------------------------------------------------
    const planBlocks = [];
    const plannedDatesSorted = Object.values(calendar)
        .filter((c) => !!c.plannedType)
        .map((c) => c.dateStr)
        .sort();

    const visited = new Set();

    plannedDatesSorted.forEach((pDateStr) => {
        if (visited.has(pDateStr)) return;

        const blockDates = [];
        const blockPlannedLeaves = [];

        let curr = parseLocalDate(pDateStr);
        while (true) {
            const currStr = formatLocalDate(curr);
            const cal = calendar[currStr];
            const isOff = cal ? cal.isInitialDayOff || !!cal.plannedType : false;
            if (!isOff) break;

            blockDates.unshift(currStr);
            if (cal.plannedType) {
                blockPlannedLeaves.unshift({
                    date: currStr,
                    type: cal.plannedType,
                    linkedCreditId: cal.linkedCreditId,
                });
                visited.add(currStr);
            }
            curr.setDate(curr.getDate() - 1);
        }

        curr = parseLocalDate(pDateStr);
        curr.setDate(curr.getDate() + 1);
        while (true) {
            const currStr = formatLocalDate(curr);
            const cal = calendar[currStr];
            const isOff = cal ? cal.isInitialDayOff || !!cal.plannedType : false;
            if (!isOff) break;

            blockDates.push(currStr);
            if (cal.plannedType) {
                blockPlannedLeaves.push({
                    date: currStr,
                    type: cal.plannedType,
                    linkedCreditId: cal.linkedCreditId,
                });
                visited.add(currStr);
            }
            curr.setDate(curr.getDate() + 1);
        }

        const uniqueBlockDates = Array.from(new Set(blockDates)).sort();
        const dayDetails = uniqueBlockDates.map((dateStr) => {
            const cal = calendar[dateStr];
            const dObj = parseLocalDate(dateStr);
            const holidayName = holidayDetails[dateStr];

            let desc = 'Working Day';
            if (cal?.plannedType) {
                desc = `Planned ${cal.plannedType === 'Compensatory_Used' ? 'Comp Off' : cal.plannedType}`;
            } else if (cal?.hasExistingLeave) {
                desc = 'Existing Leave';
            } else if (holidayName) {
                desc = holidayName;
            } else if (defaultIsSunday(dObj)) {
                desc = 'Sunday';
            } else if (defaultIsSecondSaturday(dObj)) {
                desc = 'Second Saturday';
            }

            return {
                dateStr,
                desc,
                isPlannedLeave: !!cal?.plannedType,
                plannedType: cal?.plannedType || null,
                linkedCreditId: cal?.linkedCreditId || null,
            };
        });

        planBlocks.push({
            id: pDateStr,
            startDate: uniqueBlockDates[0],
            endDate: uniqueBlockDates[uniqueBlockDates.length - 1],
            totalDays: uniqueBlockDates.length,
            dayDetails,
            plannedLeaves: blockPlannedLeaves,
        });
    });

    planBlocks.sort((a, b) => a.startDate.localeCompare(b.startDate));
    return planBlocks;
}
